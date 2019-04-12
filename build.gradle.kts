buildscript {
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
        classpath("io.fabric.tools:gradle:1.28.1")
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

tasks {
    wrapper {
        version = Versions.Gradle.runningVersion
        distributionType = Wrapper.DistributionType.BIN
    }
    val clean by registering(Delete::class) {
        delete(buildDir)
        delete("app/build")
    }
}
