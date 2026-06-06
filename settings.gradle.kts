pluginManagement {
    repositories {
        gradlePluginPortal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven { setUrl("https://maven.aliyun.com/repository/google") }
        maven { setUrl("https://maven.aliyun.com/repository/public") }
        maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { setUrl("https://maven.aliyun.com/repository/google") }
        maven { setUrl("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
    }
}

// 动态 OS 检测逻辑，自动写入正确的 sdk.dir
val localPropsFile = file("local.properties")
if (!localPropsFile.exists()) {
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    val isWsl = System.getProperty("os.version").lowercase().contains("microsoft") || 
                System.getProperty("os.version").lowercase().contains("wsl")
                
    val sdkDir = when {
        isWindows -> "C:\\\\Users\\\\${System.getProperty("user.name")}\\\\AppData\\\\Local\\\\Android\\\\Sdk"
        isWsl -> "/home/${System.getProperty("user.name")}/Android/Sdk"
        else -> "/home/${System.getProperty("user.name")}/Android/Sdk" // Default Linux/Mac
    }
    
    localPropsFile.writeText("sdk.dir=$sdkDir\n")
    println("Generated local.properties with dynamic SDK path: $sdkDir")
}

rootProject.name = "TomatoClock"
include(":app")