plugins {
    id("kotlinx-atomicfu")
    id("mokkery-publish")
    id("mokkery-multiplatform")
    alias(libs.plugins.poko)
    id("org.jetbrains.kotlinx.kover") version "0.8.3"
}

dependencies {
    commonMainApi(project(":mokkery-core"))
    commonTestImplementation(kotlin("test"))
    jvmMainImplementation(libs.objenesis)
    jvmMainImplementation(libs.bytebuddy)
}
