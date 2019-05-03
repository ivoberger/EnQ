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
        versionName = "0.2.0"
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
    implementation(Libs.statemachine)

    kapt(Libs.dagger_compiler)
    implementation(Libs.dagger)

    implementation(Libs.splitties_fun_pack_android_base)
    implementation(Libs.jwtdecode)

    implementation(Libs.okhttp)
    implementation(Libs.logging_interceptor)
    implementation(Libs.retrofit)
    implementation(Libs.retrofit2_kotlin_coroutines_adapter)
    implementation(Libs.converter_moshi)
    implementation(Libs.moshi)
    kapt(Libs.moshi_kotlin_codegen)

    testImplementation(Libs.assertj_core)

}