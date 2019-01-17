import kotlin.String

/**
 * Find which updates are available by running
 *     `$ ./gradlew buildSrcVersions`
 * This will only update the comments.
 *
 * YOU are responsible for updating manually the dependency version. */
object Versions {
    const val appcompat: String = "1.1.0-alpha01" 

    const val constraintlayout: String = "2.0.0-alpha2" 

    const val core_ktx: String = "1.0.1" 

    const val fragment_ktx: String = "1.1.0-alpha02" 

    const val com_android_tools_build_gradle: String = "3.5.0-alpha01" 

    const val lint_gradle: String = "26.5.0-alpha01" 

    const val jmusicbotandroid: String = "5aba4fe454" 

    const val timbersentry: String = "0.2.0" 

    const val com_github_triplet_play_gradle_plugin: String = "2.1.0" 

    const val material: String = "1.1.0-alpha01" 

    const val community_material_typeface: String = "3.1.0-rc02" 

    const val fastadapter_commons: String = "3.3.1" 

    const val fastadapter_extensions: String = "3.3.1" 

    const val fastadapter: String = "3.3.1" 

    const val iconics_core: String = "3.1.0" 

    const val moshi: String = "1.8.0" 

    const val picasso: String = "2.71828" 

    const val de_fayard_buildsrcversions_gradle_plugin: String = "0.3.2" 

    const val sentry_android_gradle_plugin: String = "1.7.16" 

    const val org_jetbrains_kotlin: String = "1.3.11" 

    const val kotlinx_coroutines_android: String = "1.1.0" 

    const val core_kt: String = "1.0.3" 

    /**
     *
     *   To update Gradle, edit the wrapper file at path:
     *      ./gradle/wrapper/gradle-wrapper.properties
     */
    object Gradle {
        const val runningVersion: String = "5.1.1"

        const val currentVersion: String = "5.1.1"

        const val nightlyVersion: String = "5.2-20190117003518+0000"

        const val releaseCandidate: String = ""
    }
}
