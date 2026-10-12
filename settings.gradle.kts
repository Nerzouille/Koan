pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.mozilla.org/maven2") {
            content {
                includeGroupByRegex("org\\.mozilla\\..*")
            }
        }
    }
}

rootProject.name = "Koan"
include(":app")
