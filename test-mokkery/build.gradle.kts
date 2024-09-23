import dev.mokkery.gradle.mokkery
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("dev.mokkery")
    id("org.jetbrains.kotlin.plugin.allopen")
}

allOpen {
    annotation("dev.mokkery.test.OpenForMokkery")
}

val mokkeryAllowIndirectSuperCalls: String by project

mokkery {
    allowIndirectSuperCalls.set(mokkeryAllowIndirectSuperCalls.toBoolean())
    ignoreFinalMembers.set(true)
}

kotlin {

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("coroutines") {
                group("blocking") {
                    withJvm()
                    withNative()
                }
            }
        }
    }

    jvm()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

configurations
    .filter { it.name.startsWith("wasm") }
    .forEach {
        it.resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
    }

tasks {
    named("jvmTest") {
        enabled = false
    }
    register<Test>("test") {
        description = "Unified test task for all platforms"
        group = "verification"
        val jvmTestTask = named<Test>("jvmTest").get()
        testClassesDirs = jvmTestTask.testClassesDirs
        classpath = jvmTestTask.classpath
        dependsOn("jvmTest")
    }
}



dependencies {
    commonTestImplementation(kotlin("test"))
    "coroutinesTestImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    "coroutinesTestImplementation"(mokkery("coroutines"))
}

configurations
    .filter { it.name.startsWith("wasm") }
    .forEach {
        it.resolutionStrategy.force("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
    }
