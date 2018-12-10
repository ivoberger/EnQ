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
            force("net.sf.proguard:proguard-gradle:6.1.0beta1")
        }
    }
}

plugins {
    id("jmfayard.github.io.gradle-kotlin-dsl-libs") version Versions.jmfayard_github_io_gradle_kotlin_dsl_libs_gradle_plugin
}

allprojects {
    repositories {
        google()
        jcenter()
        maven(url = "https://jitpack.io")
    }
}

tasks {
    val clean by registering(Delete::class) {
        delete(buildDir)
        delete("app/build")
    }
}
