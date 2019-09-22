import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    compileSdkVersion(29)
    defaultConfig {
        applicationId = "me.iberger.enq"
        minSdkVersion(21)
        targetSdkVersion(29)
        versionCode = 2
        versionName = "0.7.5"
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
    packagingOptions.pickFirsts = setOf(
        "META-INF/core-ktx_release.kotlin_module",
        "META-INF/atomicfu.kotlin_module",
        "META-INF/library-core_release.kotlin_module"
    )
}

tasks.withType(KotlinCompile::class.java).all {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

play {
    serviceAccountCredentials = file("play_credentials.json")
    track = "beta"
    defaultToAppBundles = true
    resolutionStrategy = "auto"
}

androidExtensions {
    isExperimental = true
}

dependencies {
    // Kotlin (extensions)
    implementation(Libs.kotlin_stdlib_jdk8)
    implementation(Libs.kotlinx_coroutines_android)
    implementation(Libs.core_ktx)
//    // appcompat, arch components etc.
    implementation(Libs.appcompat)
    implementation(Libs.recyclerview)
    implementation(Libs.lifecycle_extensions)
    implementation(Libs.lifecycle_livedata_ktx)
    implementation(Libs.preference_ktx)
    implementation(Libs.navigation_fragment_ktx)
    implementation(Libs.navigation_ui_ktx)
    implementation(Libs.constraintlayout)
    implementation(Libs.material)

    implementation("com.ivoberger:jmusicbot-client:0.8.5")

    // utils
    implementation(Libs.timber)
    implementation(Libs.crashlytics)

    implementation(Libs.moshi)

    implementation(Libs.splitties_fun_pack_android_material_components)

    implementation(Libs.fastadapter)
    implementation(Libs.fastadapter_extensions_utils)
    implementation(Libs.fastadapter_extensions_ui)
    implementation(Libs.fastadapter_extensions_drag)
    implementation(Libs.fastadapter_extensions_swipe)
    implementation(Libs.fastadapter_extensions_diff)

    implementation(Libs.iconics_core)
    implementation(Libs.community_material_typeface)
}
