pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("http://nexus.arashivision.com:9999/repository/maven-public/")
            isAllowInsecureProtocol = true
            credentials {
                username = "deployment"
                password = "test123"
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri ("https://jitpack.io")}
        maven {
            url = uri("http://nexus.arashivision.com:9999/repository/maven-public/")
            isAllowInsecureProtocol = true
            credentials {
                username = "deployment"
                password = "test123"
            }
        }
    }
}

rootProject.name = "VideoResolution"
include(":app")
 