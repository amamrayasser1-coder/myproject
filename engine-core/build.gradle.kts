plugins { alias(libs.plugins.kotlin.jvm) }
kotlin { jvmToolchain(17) }
dependencies {
    api(libs.kotlinx.coroutines.core)
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.junit.jupiter)
}
tasks.test { useJUnitPlatform() }
