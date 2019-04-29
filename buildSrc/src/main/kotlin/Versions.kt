/**
 * Find which updates are available by running
 *     `$ ./gradlew buildSrcVersions`
 * This will only update the comments.
 *
 * YOU are responsible for updating manually the dependency version. */
object Versions {
    const val appcompat: String = "1.1.0-alpha04"

    const val constraintlayout: String = "2.0.0-alpha2"

    const val androidx_core_core_ktx: String = "1.1.0-alpha05"

    const val fragment_ktx: String = "1.1.0-alpha07"

    const val lifecycle_extensions: String = "2.1.0-alpha04"

    const val androidx_navigation: String = "2.1.0-alpha01"

    const val preference_ktx: String = "1.1.0-alpha04"

    const val recyclerview: String = "1.1.0-alpha04"

    const val aapt2: String = "3.5.0-alpha12-5342889"

    const val com_android_tools_build_gradle: String = "3.5.0-alpha12"

    const val lint_gradle: String = "26.5.0-alpha12"

    const val jwtdecode: String = "1.3.0"

    const val crashlytics: String = "2.9.9"

    const val com_github_bumptech_glide: String = "4.9.0"

    const val statemachine: String = "0.1.2"

    const val com_github_triplet_play_gradle_plugin: String = "2.2.0"

    const val material: String = "1.1.0-alpha01"

    const val com_google_dagger: String = "2.22.1"

    const val firebase_core: String = "16.0.8"

    const val google_services: String = "4.2.0"

    const val retrofit2_kotlin_coroutines_adapter: String = "0.9.2"

    const val timber: String = "4.7.1"

    const val com_louiscad_splitties: String = "3.0.0-alpha04"

    const val community_material_typeface: String = "3.5.95.1"

    const val fastadapter_extensions_diff: String = "4.0.0-rc04"

    const val fastadapter_extensions_drag: String = "4.0.0-rc04"

    const val fastadapter_extensions_swipe: String = "4.0.0-rc04"

    const val fastadapter_extensions_ui: String = "4.0.0-rc04"

    const val fastadapter_extensions_utils: String = "4.0.0-rc04"

    const val fastadapter: String = "4.0.0-rc04"

    const val iconics_core: String = "3.2.5" // available: "4.0.0-b1"

    const val com_squareup_moshi: String = "1.8.0"

    const val com_squareup_okhttp3: String = "3.14.1"

    const val okio: String = "2.2.2"

    const val com_squareup_retrofit2: String = "2.5.0"

    const val de_fayard_buildsrcversions_gradle_plugin: String = "0.3.2"

    const val io_fabric_tools_gradle: String = "1.28.1"

    const val assertj_core: String = "3.12.2"

    const val org_jetbrains_kotlin: String = "1.3.31"

    const val kotlinx_coroutines_android: String = "1.2.1"

    const val ru_ztrap_iconics_core_ktx: String = "1.0.3"

    /**
     *
     *   To update Gradle, edit the wrapper file at path:
     *      ./gradle/wrapper/gradle-wrapper.properties
     */
    object Gradle {
        const val runningVersion: String = "5.4.1"

        const val currentVersion: String = "5.4.1"

        const val nightlyVersion: String = "5.5-20190429000105+0000"

        const val releaseCandidate: String = ""
    }
}
