
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask

plugins {
    alias(libs.plugins.dokka)
    id("org.jetbrains.kotlinx.kover") version "0.8.3"
}

buildscript {
    repositories {
        mavenCentral()
        google()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }

    dependencies {
        classpath(":build-mokkery")
        classpath(libs.gradle.plugin.kotlinx.atomicfu)
        classpath(libs.dokka.base)
    }
}


rootProject.version = libs.versions.mokkery.get()
rootProject.group = "dev.mokkery"

rootProject.ext["pluginId"] = "dev.mokkery"

allprojects {
    group = rootProject.group
    version = rootProject.version
    afterEvaluate {
        extensions.findByType<JavaPluginExtension>()?.apply {
            toolchain.languageVersion.set(JavaLanguageVersion.of(8))
        }
    }
}

val dokkaHtmlMultiModule by tasks.getting(DokkaMultiModuleTask::class) {
    moduleName.set("Mokkery")
    moduleVersion.set(libs.versions.mokkery.get())
    pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
        customAssets += rootProject.layout.projectDirectory.file("website/static/img/logo-icon.svg").asFile
    }
}
