import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Local release signing. keystore.properties (gitignored) points at a keystore outside the repo.
// Without it, release builds fall back to the debug key so they still install.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}

android {
    namespace = "dev.nerzouille.koan"
    // AC 157 requires compiling against 37.1 (minor SDK release).
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }

    defaultConfig {
        applicationId = "dev.nerzouille.koan"
        minSdk = 34
        targetSdk = 37
        // versionCode only goes up: 2 was the m3 build already on the phone.
        versionCode = 3
        versionName = "0.1.0"
        // GeckoView-omni ships every ABI; both owner phones are arm64.
        ndk { abiFilters += "arm64-v8a" }
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
        // The release build under its own id, debug-signed: a clean side install for first-run
        // checks that never touches the real app's data.
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
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
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment)
    // Passwords: reveal/copy only after the fingerprint or the device PIN (approved for M5).
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidsvg)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.geckoview)
    implementation(libs.mozilla.browser.engine.gecko)
    implementation(libs.mozilla.browser.state)
    implementation(libs.mozilla.concept.engine)
    implementation(libs.mozilla.feature.session)
    implementation(libs.mozilla.feature.tabs)
    implementation(libs.mozilla.browser.session.storage)
    implementation(libs.mozilla.lib.state)
    implementation(libs.mozilla.feature.readerview)
    implementation(libs.mozilla.feature.downloads)
    // Already shipped with feature-downloads; declared to compile against its DownloadFileUtils.
    implementation(libs.mozilla.support.utils)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    testImplementation(libs.kotlinx.coroutines.test)
}
