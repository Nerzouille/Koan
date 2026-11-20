plugins { `java-gradle-plugin` }

repositories {
    google()
    mavenCentral()
}

dependencies {
    compileOnly("com.android.tools.build:gradle-api:9.4.1") {
        isTransitive = false
    }
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.4.20")
    implementation("org.ow2.asm:asm:9.9.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.android.tools.build:gradle-api:9.4.1") { isTransitive = false }
}

gradlePlugin {
    plugins {
        create("passkeys") {
            id = "koan.passkeys"
            implementationClass = "koan.passkeys.PasskeyCompatibilityPlugin"
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
