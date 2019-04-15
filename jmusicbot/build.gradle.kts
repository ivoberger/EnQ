plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
}

android {
    compileSdkVersion(28)
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(28)
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    packagingOptions.pickFirst("META-INF/atomicfu.kotlin_module")
}

dependencies {
    implementation(Libs.kotlin_stdlib_jdk8)
    implementation(Libs.kotlinx_coroutines_android)
    implementation(Libs.androidx_core_core_ktx)
    implementation(Libs.lifecycle_extensions)

    implementation(Libs.timber)

    kapt(Libs.dagger_compiler)
    implementation(Libs.dagger)

    implementation(Libs.splitties_systemservices)
    implementation(Libs.splitties_preferences)
    implementation(Libs.jwtdecode)

    implementation(Libs.okhttp)
    implementation("com.squareup.okhttp3:logging-interceptor:${Versions.okhttp}")
    implementation(Libs.retrofit)
    implementation(Libs.retrofit2_kotlin_coroutines_adapter)
    implementation(Libs.converter_moshi)
    implementation(Libs.moshi)
    kapt(Libs.moshi_kotlin_codegen)
}