plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("com.github.triplet.play") version Versions.com_github_triplet_play_gradle_plugin
    id("io.sentry.android.gradle")
}

android {
    compileSdkVersion(28)
    defaultConfig {
        applicationId = "me.iberger.enq"
        minSdkVersion(21)
        targetSdkVersion(28)
        versionCode = 2
        versionName = "0.5.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore.jks")
            storePassword = System.getenv("SIGNING_KEYSTORE_PW")
            keyAlias = "enq"
            keyPassword = System.getenv("SIGNING_KEY_PW")
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = " (debug)"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    sourceSets {
        getByName("main").res.srcDirs("src/main/resources", "src/main/res")
    }
    lintOptions {
        isAbortOnError = false
    }
}

play {
    serviceAccountCredentials = file("play_credentials.json")
    track = "beta"
    defaultToAppBundles = true
    resolutionStrategy = "auto"
}

sentry {
    autoProguardConfig = true
    autoUpload = true
}

dependencies {
    // Kotlin (extensions)
    implementation(Libs.kotlin_stdlib_jdk8)
    implementation(Libs.kotlinx_coroutines_android)
    implementation(Libs.androidx_core_core_ktx)
    implementation(Libs.fragment_ktx)
    // appcompat, arch components etc.
    implementation(Libs.appcompat)
    implementation(Libs.recyclerview)
    implementation(Libs.lifecycle_extensions)
    implementation(Libs.preference_ktx)
    implementation(Libs.navigation_fragment_ktx)
    implementation(Libs.navigation_ui_ktx)
    implementation(Libs.constraintlayout)
    implementation(Libs.material)

    implementation(project(":jmusicbot"))

    // utils
    implementation(Libs.timbersentry)

    implementation(Libs.glide)
    kapt(Libs.com_github_bumptech_glide_compiler)
    implementation(Libs.moshi)
    implementation(Libs.okio)

    implementation(Libs.splitties_views_appcompat)
    implementation(Libs.splitties_toast)
    implementation(Libs.splitties_material_colors)
    implementation(Libs.splitties_resources)

    implementation(Libs.fastadapter)
    implementation(Libs.fastadapter_extensions_utils)
    implementation(Libs.fastadapter_extensions_ui)
    implementation(Libs.fastadapter_extensions_drag)
    implementation(Libs.fastadapter_extensions_swipe)
    implementation(Libs.fastadapter_extensions_diff)
    implementation(Libs.aboutlibraries)

    implementation(Libs.iconics_core)
    implementation(Libs.community_material_typeface)
    implementation(Libs.ru_ztrap_iconics_core_ktx)
}
