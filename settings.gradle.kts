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
        maven { url = uri("https://android-sdk.is.com/") }
        google()
        mavenCentral()
    }
}

rootProject.name = "LocalDrop"
include(":app")
