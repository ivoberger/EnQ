/**
 * Find which updates are available by running
 *     `$ ./gradlew buildSrcVersions`
 * This will only update the comments.
 *
 * YOU are responsible for updating manually the dependency version.
 */
object Versions {
    const val appcompat: String = "1.1.0-rc01"

    const val constraintlayout: String = "2.0.0-beta1"

    const val core_ktx: String = "1.1.0-rc02"

    const val fragment_ktx: String = "1.2.0-alpha01"

    const val androidx_lifecycle: String = "2.2.0-alpha02"

    const val androidx_navigation: String = "2.1.0-beta02"

    const val preference_ktx: String = "1.1.0-rc01"

    const val recyclerview: String = "1.1.0-beta01"

    const val aapt2: String = "3.5.0-rc01-5435860"

    const val com_android_tools_build_gradle: String = "3.5.0-rc01"

    const val lint_gradle: String = "26.5.0-rc01"

    const val jwtdecode: String = "1.3.0"

    const val crashlytics: String = "2.10.1"

    const val com_diffplug_gradle_spotless_gradle_plugin: String = "3.23.1"

    const val com_github_bumptech_glide: String = "4.9.0"

    const val statemachine: String = "0.2.0"

    const val com_github_triplet_play_gradle_plugin: String = "2.3.0"

    const val material: String = "1.1.0-alpha07"

    const val com_google_dagger: String = "2.24"

    const val firebase_core: String = "17.0.1"

    const val google_services: String = "4.3.0"

    const val retrofit2_kotlin_coroutines_adapter: String = "0.9.2"

    const val timber: String = "4.7.1"

    const val com_louiscad_splitties: String = "3.0.0-alpha06"

    const val community_material_typeface: String = "3.5.95.1-kotlin" // available: "3.5.95.1"

    const val fastadapter_extensions_diff: String = "4.0.1" // available: "4.1.0-b01"

    const val fastadapter_extensions_drag: String = "4.0.1" // available: "4.1.0-b01"

    const val fastadapter_extensions_swipe: String = "4.0.1" // available: "4.1.0-b01"

    const val fastadapter_extensions_ui: String = "4.0.1" // available: "4.1.0-b01"

    const val fastadapter_extensions_utils: String = "4.0.1" // available: "4.1.0-b01"

    const val fastadapter: String = "4.0.1" // available: "4.1.0-b01"

    const val iconics_core: String = "4.0.0" // available: "4.0.1-b01"

    const val com_squareup_moshi: String = "1.8.0"

    const val com_squareup_okhttp3: String = "4.0.1"

    const val okio: String = "2.2.2"

    const val com_squareup_retrofit2: String = "2.6.0"

    const val de_fayard_buildsrcversions_gradle_plugin: String = "0.3.2"

    const val io_fabric_tools_gradle: String = "1.31.0"

    const val assertj_core: String = "3.12.2"

    const val org_jetbrains_kotlin: String = "1.3.41"

    const val kotlinx_coroutines_android: String = "1.2.1" // available: "1.3.0-RC-1.3.50-eap-5"

    /**
     *
     *   To update Gradle, edit the wrapper file at path:
     *      ./gradle/wrapper/gradle-wrapper.properties
     */
    object Gradle {
        const val runningVersion: String = "5.5.1"

        const val currentVersion: String = "5.5.1"

        const val nightlyVersion: String = "5.7-20190726220034+0000"

        const val releaseCandidate: String = ""
    }
}
