import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

sourceSets {
    main {
        kotlin.srcDirs("src/jvmMain/kotlin")
        resources.srcDirs("src/jvmMain/resources")
    }
    test {
        kotlin.srcDirs("src/test/kotlin")
        resources.srcDirs("src/test/resources")
    }
}

dependencies {
    implementation(project(":shared-core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.foundation)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jsoup:jsoup:1.18.1")
    implementation("net.jthink:jaudiotagger:2.2.5")
    implementation("org.openjfx:javafx-base:21.0.7:win")
    implementation("org.openjfx:javafx-graphics:21.0.7:win")
    implementation("org.openjfx:javafx-media:21.0.7:win")
    implementation("org.openjfx:javafx-swing:21.0.7:win")
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "com.yaneodex.desktop.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "YaNeoDex Desktop"
            packageVersion = "0.1.1"
            description = "Windows desktop client for the YaNeoDex ecosystem"
            modules(
                "java.desktop",
                "java.net.http",
                "java.logging",
                "java.scripting",
                "jdk.unsupported",
            )
            windows {
                upgradeUuid = "b5db8ab8-0b47-4b2c-9e75-7d719a646f0c"
                menuGroup = "YaNeoDex Desktop"
            }
        }
    }
}
