import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.BuiltArtifactsLoader
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.util.Properties
import javax.inject.Inject
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("koan.passkeys")
}

// Local release signing. keystore.properties (gitignored) points at a keystore outside the repo.
// Without it, release builds fall back to the debug key so they still install.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}

android {
    namespace = "dev.nerzouille.koan"
    // AC 160 requires compiling against 37.2 (minor SDK release).
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "dev.nerzouille.koan"
        // Android 11: no live blur below 12 (DeviceProfile.liveBlur); arm64 only all the same.
        minSdk = 30
        targetSdk = 37
        // versionCode only goes up: 2 was the m3 build already on the phone, 7 the Android 11
        // preview (0.2.2-preview.1).
        versionCode = 9
        versionName = "0.4.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // GeckoView-omni ships every ABI; both owner phones are arm64. `-Pkoan.emulatorAbi` adds
        // x86_64 for emulator test runs only, never for a build that goes on a phone.
        ndk {
            abiFilters += "arm64-v8a"
            if (project.hasProperty("koan.emulatorAbi")) abiFilters += "x86_64"
        }
    }

    signingConfigs {
        if (keystoreProps["storeFile"] != null) {
            create("release") {
                storeFile = file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            proguardFiles("proguard-rules.pro")
            signingConfig = if (keystoreProps["storeFile"] != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
        // Fast test installs (CLAUDE.md "Test builds"): release-signed, same id, so it installs over
        // the real app and its data; not debuggable; but no R8 and no resource shrinking, so an
        // incremental build is quick. Never for performance checks or releases.
        create("dev") {
            initWith(getByName("release"))
            optimization {
                enable = false
            }
            isDebuggable = false
            matchingFallbacks += "release"
        }
        // The release build under its own id ("Koan Test"), debug-signed: a side install with its
        // own data and account, for first-run checks and the two-way sync tests on the test
        // Mozilla account (docs/SYNC_WRITE.md "Test plan"). Never touches the real app's data.
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            // Identify a local test APK without changing the upstream release version.
            versionNameSuffix = providers.gradleProperty("koan.testVersionSuffix").orNull
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        // BuildConfig.DEBUG gates the dev menu items.
        buildConfig = true
    }
    testOptions {
        // Ported sync code calls android.util.Log; JVM tests get no-op stubs.
        unitTests.isReturnDefaultValues = true
        // Without reference/Z-Sync the fixture tests skip instead of failing (see README).
        val contractPresent = rootProject.file("reference/Z-Sync/shared/contract").isDirectory
        unitTests.all { it.systemProperty("koan.zsync.contract", contractPresent) }
    }
    sourceSets {
        getByName("test") {
            // Z-Sync's golden fixtures, read in place (never copied or edited). Leaf dirs so the
            // JSON lands at the classpath root, as Z-Sync's FixtureLoader expects.
            val contract = rootProject.file("reference/Z-Sync/shared/contract")
            listOf("fixtures/crypto", "fixtures/wire", "fixtures/auth", "http")
                .forEach { resources.srcDir(contract.resolve(it)) }
        }
    }
    // UI strings are English only (CLAUDE.md): the libraries' translations stay out.
    androidResources {
        localeFilters += "en"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // Never loaded: megazord is Nimbus's native side (Koan runs no experiments; only its
            // Kotlin flag classes are used). ~22 MB off the APK and the install. Gecko's crash
            // libraries stay: GeckoView starts its CrashHelper process at every launch, and that
            // process crashes without libcrashhelper.so (tried 2026-10-29).
            excludes += "**/libmegazord.so"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

/**
 * Fails a shrunk build (release, staging) whose APK lost Zen space icons: ZenIcon loads the zen_*
 * drawables by name, which the resource shrinker can't see (res/raw/keep.xml keeps them).
 */
abstract class CheckZenIcons : DefaultTask() {
    @get:InputFiles abstract val apkDir: DirectoryProperty
    @get:Internal abstract val loader: Property<BuiltArtifactsLoader>
    @get:InputFile abstract val aapt2: RegularFileProperty
    @get:Input abstract val expected: Property<Int>
    @get:OutputFile abstract val report: RegularFileProperty
    @get:Inject abstract val exec: ExecOperations

    @TaskAction fun check() {
        val apk = loader.get().load(apkDir.get())?.elements?.firstOrNull()?.outputFile
            ?: throw GradleException("checkZenIcons: no APK found")
        val out = ByteArrayOutputStream()
        exec.exec {
            commandLine(aapt2.get().asFile.absolutePath, "dump", "resources", apk)
            standardOutput = out
        }
        val found = Regex("""drawable/zen_\w+""").findAll(out.toString()).map { it.value }.toSet().size
        if (found < expected.get()) {
            throw GradleException("checkZenIcons: $found of ${expected.get()} zen_* drawables in $apk (res/raw/keep.xml?)")
        }
        report.get().asFile.writeText("zen_* drawables: $found of ${expected.get()}\n")
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { registerZenIconCheck(it) }
    onVariants(selector().withBuildType("staging")) { registerZenIconCheck(it) }
}

fun registerZenIconCheck(variant: com.android.build.api.variant.ApplicationVariant) {
    val name = variant.name.replaceFirstChar { it.uppercase() }
    val expectedIcons = fileTree("src/main/res/drawable") { include("zen_*.xml") }.files.size
    val check = tasks.register<CheckZenIcons>("check${name}ZenIcons") {
        apkDir.set(variant.artifacts.get(SingleArtifact.APK))
        loader.set(variant.artifacts.getBuiltArtifactsLoader())
        aapt2.set(
            androidComponents.sdkComponents.sdkDirectory.map {
                it.dir("build-tools/${android.buildToolsVersion}").file("aapt2")
            },
        )
        expected.set(expectedIcons)
        report.set(layout.buildDirectory.file("reports/zenIcons/${variant.name}.txt"))
    }
    tasks.matching { it.name == "assemble$name" }.configureEach { dependsOn(check) }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.material3)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment)
    // Passwords: reveal/copy only after the fingerprint or the device PIN (approved for M5).
    implementation(libs.androidx.biometric)
    // Custom Tabs provider (KoanCustomTabsService); android-components already brings the same version.
    implementation(libs.androidx.browser)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidsvg)
    // QR sign-in: decodes the camera frames (no UI, no Play Services).
    implementation(libs.zxing.core)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.geckoview)
    // Already part of GeckoView's runtime dependency graph; named here for the FIDO task bridge.
    implementation(libs.gms.tasks)
    implementation(libs.mozilla.browser.engine.gecko)
    implementation(libs.mozilla.browser.state)
    implementation(libs.mozilla.concept.engine)
    implementation(libs.mozilla.feature.session)
    implementation(libs.mozilla.feature.tabs)
    implementation(libs.mozilla.browser.session.storage)
    implementation(libs.mozilla.lib.state)
    implementation(libs.mozilla.feature.readerview)
    implementation(libs.mozilla.feature.downloads)
    // Background audio: the media notification and lock screen controls.
    implementation(libs.mozilla.feature.media)
    // Login prompts carry its Login types (approved for M5).
    implementation(libs.mozilla.concept.storage)
    // Already shipped with feature-downloads; declared to compile against its DownloadFileUtils.
    implementation(libs.mozilla.support.utils)
    // Already shipped with feature-readerview; declared for uBlock Origin (WebExtensionSupport).
    implementation(libs.mozilla.support.webextensions)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation("androidx.test:core-ktx:1.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
