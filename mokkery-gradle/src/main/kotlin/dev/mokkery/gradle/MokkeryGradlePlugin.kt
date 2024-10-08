package dev.mokkery.gradle

import dev.mokkery.MokkeryCompilerDefaults
import dev.mokkery.MokkeryConfig
import dev.mokkery.MokkeryConfig.RUNTIME_DEPENDENCY
import dev.mokkery.MokkeryConfig.VERSION
import dev.mokkery.verify.VerifyModeSerializer
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion

/**
 * Configures Mokkery in source sets specified by [MokkeryGradleExtension.rule]. It includes:
 * * Adding runtime dependency
 * * Adding configured compiler plugin
 */
public class MokkeryGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        target.checkKotlinSetup()
        val mokkery = target.extensions.create("mokkery", MokkeryGradleExtension::class.java)
        mokkery.defaultMockMode.convention(MokkeryCompilerDefaults.mockMode)
        mokkery.defaultVerifyMode.convention(MokkeryCompilerDefaults.verifyMode)
        mokkery.rule.convention(ApplicationRule.AllTests)
        mokkery.allowIndirectSuperCalls.convention(false)
        mokkery.ignoreInlineMembers.convention(false)
        mokkery.ignoreFinalMembers.convention(false)
        target.configureDependencies()
        super.apply(target)
    }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> = kotlinCompilation.run {
        target.project.provider {
            listOf(
                SubpluginOption(key = "mockMode", value = project.mokkery.defaultMockMode.get().toString()),
                SubpluginOption(
                    key = "verifyMode",
                    value = VerifyModeSerializer.serialize(project.mokkery.defaultVerifyMode.get())
                ),
                SubpluginOption(
                    key = "allowIndirectSuperCalls",
                    value = project.mokkery.allowIndirectSuperCalls.get().toString()
                ),
                SubpluginOption(
                    key = "ignoreFinalMembers",
                    value = project.mokkery.ignoreFinalMembers.get().toString()
                ),
                SubpluginOption(
                    key = "ignoreInlineMembers",
                    value = project.mokkery.ignoreInlineMembers.get().toString()
                )
            )
        }
    }

    override fun getCompilerPluginId(): String = MokkeryConfig.PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact {
        return SubpluginArtifact(
            groupId = MokkeryConfig.GROUP,
            artifactId = MokkeryConfig.PLUGIN_ARTIFACT_ID,
            version = VERSION,
        )
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = kotlinCompilation
        .project
        .mokkery
        .rule
        .get()
        .isApplicable(kotlinCompilation.defaultSourceSet)

    private fun Project.checkKotlinSetup() {
        if (extensions.findByName("kotlin") == null) {
            error("Kotlin plugin not applied! Mokkery requires kotlin plugin!")
        }
        val kotlinVersion = kotlinToolingVersion.toString()
        if (kotlinVersion.startsWith("1.")) {
            error("Current Kotlin version must be at least 2.0.0, but is $kotlinVersion!")
        }
    }

    private fun Project.configureDependencies() {
        afterEvaluate {
            // https://youtrack.jetbrains.com/issue/KT-53477/Native-Gradle-plugin-doesnt-add-compiler-plugin-transitive-dependencies-to-compiler-plugin-classpath
            configurations.matching {
                it.name.startsWith("kotlin") && it.name.contains("CompilerPluginClasspath")
            }.all {
                it.isTransitive = true
            }
            val rule = mokkery.rule.get()
            val applicableSourceSets = kotlinExtension
                .sourceSets
                .filter { rule.isApplicable(it) }
            applicableSourceSets
                .filter { sourceSet -> sourceSet.dependsOn.none { it in applicableSourceSets  } }
                .forEach {
                    mokkeryInfo("Runtime dependency $RUNTIME_DEPENDENCY applied to sourceSet: ${it.name}! ")
                    it.dependencies {
                        implementation(RUNTIME_DEPENDENCY)
                    }
                }
        }
    }

    private val Project.mokkery get() = extensions.getByType(MokkeryGradleExtension::class.java)
}
