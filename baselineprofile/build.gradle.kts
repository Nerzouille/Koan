import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Generates :app's baseline profile on a connected phone (docs/HANDOFF.md "Baseline profile").
// It drives a separate copy of Koan (dev.nerzouille.koan.bp, its own data), never the real app.
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "dev.nerzouille.koan.baselineprofile"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        // Profiles are collected without root from Android 13 (API 33) on.
        minSdk = 33
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}

// The app id of the copy under test, for the generator (it never targets the real app).
androidComponents {
    onVariants { variant ->
        val loader = variant.artifacts.getBuiltArtifactsLoader()
        variant.instrumentationRunnerArguments.put("targetAppId", variant.testedApks.map { loader.load(it)?.applicationId ?: "" })
    }
}
