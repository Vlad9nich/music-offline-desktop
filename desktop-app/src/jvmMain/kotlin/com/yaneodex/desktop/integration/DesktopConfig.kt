package com.yaneodex.desktop.integration

import java.io.File
import java.util.Properties

data class DesktopConfig(
    val libraryPath: String?,
    val ocrBaseUrl: String?,
    val ocrToken: String?,
    val downloadDir: String?,
) {
    companion object {
        fun load(projectRoot: File = File(System.getProperty("user.dir"))): DesktopConfig {
            val envFile = File(projectRoot, ".env")
            val fromFile = if (envFile.exists()) loadDotEnv(envFile) else emptyMap()
            fun pick(key: String): String? = System.getenv(key)?.takeIf { it.isNotBlank() } ?: fromFile[key]?.takeIf { it.isNotBlank() }

            return DesktopConfig(
                libraryPath = pick("YANEODEX_LIBRARY_PATH"),
                ocrBaseUrl = pick("YANEODEX_OCR_BASE_URL"),
                ocrToken = pick("YANEODEX_OCR_TOKEN"),
                downloadDir = pick("YANEODEX_DOWNLOAD_DIR"),
            )
        }

        private fun loadDotEnv(file: File): Map<String, String> {
            val properties = Properties()
            file.readLines()
                .map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
                .forEach { line ->
                    val split = line.indexOf('=')
                    if (split > 0) {
                        val key = line.substring(0, split).trim()
                        val value = line.substring(split + 1).trim().trim('"').trim('\'')
                        properties.setProperty(key, value)
                    }
                }
            return properties.stringPropertyNames().associateWith { properties.getProperty(it) }
        }
    }
}
