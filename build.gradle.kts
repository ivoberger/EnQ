buildscript {
    var kotlin_version: String by extra
    kotlin_version = "1.3.30"
    repositories {
        google()
        jcenter()
        maven(url = "https://maven.fabric.io/public")
    }
    dependencies {
        classpath(Libs.com_android_tools_build_gradle)
        classpath(Libs.kotlin_gradle_plugin)
        classpath(Libs.google_services)
        classpath(Libs.com_github_triplet_play_gradle_plugin)
        classpath(Libs.io_fabric_tools_gradle)
    }
}

plugins {
    id("de.fayard.buildSrcVersions") version Versions.de_fayard_buildsrcversions_gradle_plugin
}

allprojects {
    repositories {
        google()
        jcenter()
        maven(url = "https://jitpack.io")
    }
}

//subprojects {
//    pluginManager.withPlugin("kotlin-kapt") {
//        configure<KaptExtension> {
//            useBuildCache = true
//        }
//    }
//}

tasks {
    wrapper {
        version = Versions.Gradle.runningVersion
        distributionType = Wrapper.DistributionType.ALL
    }
    val clean by registering(Delete::class) {
        delete(buildDir)
        delete("app/build")
    }
}
