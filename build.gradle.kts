buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath(Libs.com_android_tools_build_gradle)
        classpath(Libs.kotlin_gradle_plugin)
        classpath(Libs.sentry_android_gradle_plugin)
    }
    configurations.all {
        resolutionStrategy {
            force("net.sf.proguard:proguard-gradle:6.1.0beta2")
        }
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

tasks.wrapper {
    version = Versions.Gradle.runningVersion
    distributionType = Wrapper.DistributionType.BIN
}

tasks {
    val clean by registering(Delete::class) {
        delete(buildDir)
        delete("app/build")
    }
}
