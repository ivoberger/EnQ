import kotlin.String

/**
 * Find which updates are available by running
 *     `$ ./gradlew syncLibs`
 * This will only update the comments.
 *
 * YOU are responsible for updating manually the dependency version. */
object Versions {
    const val appcompat: String = "1.1.0-alpha01" // exceed the version found: 1.0.2

    const val constraintlayout: String = "2.0.0-alpha2" // exceed the version found: 1.1.3

    const val core_ktx: String = "1.0.1" 

    const val fragment_ktx: String = "1.1.0-alpha02" // exceed the version found: 1.0.0

    const val com_android_tools_build_gradle: String =
            "3.4.0-alpha07" // exceed the version found: 3.2.1

    const val lint_gradle: String = "26.4.0-alpha07" // exceed the version found: 26.2.1

    const val jmusicbotandroid: String = "5aba4fe454" // exceed the version found: 0.2.0

    const val timbersentry: String = "0.2.0" 

    const val com_github_triplet_play_gradle_plugin: String = "2.0.0" 

    const val material: String = "1.1.0-alpha01" // exceed the version found: 1.0.0

    const val community_material_typeface: String =
            "3.1.0-rc02" // exceed the version found: 2.7.94.1

    const val fastadapter_commons: String = "3.3.1" 

    const val fastadapter_extensions: String = "3.3.1" 

    const val fastadapter: String = "3.3.1" 

    const val iconics_core: String = "3.1.0" 

    const val moshi: String = "1.8.0" 

    const val picasso: String = "2.71828" 

    const val sentry_android_gradle_plugin: String = "1.7.15" 

    const val jmfayard_github_io_gradle_kotlin_dsl_libs_gradle_plugin: String = "0.2.6" 

    const val org_jetbrains_kotlin: String = "1.3.11" // exceed the version found: 1.3.10

    const val kotlinx_coroutines_android: String = "1.0.1" 

    const val core_kt: String = "1.0.3" 

    /**
     *
     *   To update Gradle, edit the wrapper file at path:
     *      ./gradle/wrapper/gradle-wrapper.properties
     */
    object Gradle {
        const val runningVersion: String = "5.0"

        const val currentVersion: String = "5.0"

        const val nightlyVersion: String = "5.2-20181210000034+0000"

        const val releaseCandidate: String = ""
    }
}
