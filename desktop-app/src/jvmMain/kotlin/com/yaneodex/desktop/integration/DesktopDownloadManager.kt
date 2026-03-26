package com.yaneodex.desktop.integration

import com.yaneodex.core.model.DownloadBlueprint
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class DesktopDownloadManager {
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(20))
        .build()

    fun download(blueprint: DownloadBlueprint, targetDirectory: File): File {
        targetDirectory.mkdirs()
        val targetFile = uniqueTarget(targetDirectory, sanitizeFilename(blueprint.suggestedFilename))
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(blueprint.url))
            .timeout(Duration.ofMinutes(3))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0 YaNeoDexDesktop/0.1")
            .GET()

        blueprint.refererUrl?.takeIf { it.isNotBlank() }?.let { requestBuilder.header("Referer", it) }

        val tempFile = File(targetDirectory, "${targetFile.name}.part")
        val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream())
        require(response.statusCode() in 200..299) { "Download HTTP ${response.statusCode()}" }

        response.body().use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        tempFile.toPath().toFile().renameTo(targetFile)
        if (!targetFile.exists()) {
            tempFile.toPath().toFile().copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }
        return targetFile
    }

    private fun uniqueTarget(directory: File, filename: String): File {
        val extension = filename.substringAfterLast('.', "")
        val basename = if (extension.isBlank()) filename else filename.removeSuffix(".$extension")
        var attempt = 0
        while (true) {
            val suffix = if (attempt == 0) "" else " ($attempt)"
            val candidateName = if (extension.isBlank()) "$basename$suffix" else "$basename$suffix.$extension"
            val candidate = File(directory, candidateName)
            if (!candidate.exists()) return candidate
            attempt += 1
        }
    }

    private fun sanitizeFilename(filename: String): String {
        val normalized = filename.trim().ifBlank { "track.mp3" }
        val cleaned = normalized.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return if (cleaned.contains('.')) cleaned else "$cleaned.mp3"
    }
}
