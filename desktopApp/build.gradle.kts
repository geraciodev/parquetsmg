import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.geraciodev.parquetsmg.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "ParquetsMG"
            packageVersion = "1.0.0"
            description = "Parquet File Viewer"

            vendor = "geraciodev"

            linux {
                shortcut = true
                menuGroup = "Office"
            }

            windows {
                shortcut = true
                menuGroup = "ParquetsMG"
            }

            macOS {
                bundleID = "com.geraciodev.parquetsmg"
                dockName = "ParquetsMG"
            }

            val parquetProps = project.rootProject.file("parquet.properties")
            tasks.withType<AbstractJPackageTask>().configureEach {
                if (parquetProps.exists()) {
                    freeArgs.add("--file-associations")
                    freeArgs.add(parquetProps.absolutePath)
                }
            }
        }
    }
}
