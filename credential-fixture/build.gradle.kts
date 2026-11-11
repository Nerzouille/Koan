plugins { alias(libs.plugins.android.application) }

android {
    namespace = "dev.nerzouille.koan.auth.testing"
    compileSdk { version = release(37) { minorApiLevel = 1 } }
    defaultConfig {
        applicationId = "dev.nerzouille.koan.credentials.fixture"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.credentials:credentials:1.5.0")
    implementation(libs.androidx.browser)
}
