plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("com.google.gms.google-services")
    id("io.fabric")
    id("com.github.triplet.play")
}

android {
    compileSdkVersion(28)
    defaultConfig {
        applicationId = "me.iberger.enq"
        minSdkVersion(21)
        targetSdkVersion(28)
        versionCode = 2
        versionName = "0.6.5"
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
            ext["enableCrashlytics"] = false
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
    packagingOptions.pickFirsts = setOf("META-INF/core-ktx_release.kotlin_module", "META-INF/atomicfu.kotlin_module")
}

play {
    serviceAccountCredentials = file("play_credentials.json")
    track = "beta"
    defaultToAppBundles = true
    resolutionStrategy = "auto"
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
    implementation(Libs.timber)
    implementation(Libs.firebase_core)
    implementation(Libs.crashlytics)

    implementation(Libs.glide)
    implementation(Libs.okhttp3_integration)
    kapt(Libs.com_github_bumptech_glide_compiler)
    implementation(Libs.moshi)
    implementation(Libs.okio)

    implementation(Libs.splitties_fun_pack_android_material_components)

    implementation(Libs.fastadapter)
    implementation(Libs.fastadapter_extensions_utils)
    implementation(Libs.fastadapter_extensions_ui)
    implementation(Libs.fastadapter_extensions_drag)
    implementation(Libs.fastadapter_extensions_swipe)
    implementation(Libs.fastadapter_extensions_diff)

    implementation(Libs.iconics_core)
    implementation(Libs.community_material_typeface)
    implementation(Libs.ru_ztrap_iconics_core_ktx)
}