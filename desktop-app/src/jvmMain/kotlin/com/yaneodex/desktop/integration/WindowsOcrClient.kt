package com.yaneodex.desktop.integration

import com.yaneodex.core.contracts.OcrCreateJobResult
import com.yaneodex.core.contracts.OcrImportClient
import com.yaneodex.core.contracts.OcrJobResult
import com.yaneodex.core.importer.RecognizedTrackCandidate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class WindowsOcrClient : OcrImportClient {
    private val json = Json { ignoreUnknownKeys = true }

    fun submitJobFiles(serverUrl: String, authToken: String, files: List<File>): OcrCreateJobResult {
        require(files.isNotEmpty()) { "No files selected for OCR." }
        val content = files.map { it.name to it.readBytes() }
        return submitJob(serverUrl, authToken, content)
    }

    fun submitSingleImageFile(serverUrl: String, authToken: String, file: File): OcrJobResult {
        return submitSingleImage(serverUrl, authToken, file.readBytes(), file.name)
    }

    override fun submitJob(serverUrl: String, authToken: String, files: List<Pair<String, ByteArray>>): OcrCreateJobResult {
        require(files.isNotEmpty()) { "No files selected for OCR." }
        val boundary = "----YaNeoDexDesktop${System.currentTimeMillis()}"
        val payload = withRetry {
            val connection = openConnection("${normalizeServerUrl(serverUrl)}/v1/playlist-import/jobs", authToken).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            DataOutputStream(connection.outputStream).use { output ->
                files.forEach { (filename, bytes) ->
                    output.writeBytes("--$boundary\r\n")
                    output.writeBytes("Content-Disposition: form-data; name=\"files\"; filename=\"$filename\"\r\n")
                    output.writeBytes("Content-Type: image/jpeg\r\n\r\n")
                    output.write(bytes)
                    output.writeBytes("\r\n")
                }
                output.writeBytes("--$boundary--\r\n")
                output.flush()
            }
            readOrThrow(connection, "OCR")
        }
        return json.decodeFromString<OcrCreateJobResponse>(payload).toResult()
    }

    override fun submitSingleImage(serverUrl: String, authToken: String, fileBytes: ByteArray, filename: String): OcrJobResult {
        val boundary = "----YaNeoDexDesktop${System.currentTimeMillis()}"
        val payload = withRetry {
            val connection = openConnection("${normalizeServerUrl(serverUrl)}/v1/ocr-image", authToken).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }
            DataOutputStream(connection.outputStream).use { output ->
                output.writeBytes("--$boundary\r\n")
                output.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$filename\"\r\n")
                output.writeBytes("Content-Type: image/jpeg\r\n\r\n")
                output.write(fileBytes)
                output.writeBytes("\r\n")
                output.writeBytes("--$boundary--\r\n")
                output.flush()
            }
            readOrThrow(connection, "OCR")
        }
        return json.decodeFromString<OcrJobResponse>(payload).toResult()
    }

    override fun pollJob(serverUrl: String, authToken: String, jobId: String): OcrJobResult {
        repeat(60) {
            val payload = withRetry {
                val connection = openConnection("${normalizeServerUrl(serverUrl)}/v1/playlist-import/jobs/$jobId", authToken)
                readOrThrow(connection, "OCR polling")
            }
            val response = json.decodeFromString<OcrJobResponse>(payload).toResult()
            when (response.status.lowercase()) {
                "completed" -> return response
                "failed" -> error(response.error ?: "OCR job failed.")
            }
            Thread.sleep(1_200L)
        }
        error("OCR service did not finish in time.")
    }

    private fun <T> withRetry(
        attempts: Int = 3,
        initialBackoffMs: Long = 400L,
        call: () -> T,
    ): T {
        var lastError: Throwable? = null
        var backoff = initialBackoffMs
        repeat(attempts) { attempt ->
            try {
                return call()
            } catch (error: Throwable) {
                val retryable = error is SocketTimeoutException || error is java.io.IOException
                lastError = error
                if (!retryable || attempt == attempts - 1) {
                    throw error
                }
                Thread.sleep(backoff)
                backoff *= 2
            }
        }
        throw lastError ?: error("Unexpected OCR retry state.")
    }

    private fun openConnection(url: String, authToken: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 180_000
            doInput = true
            setRequestProperty("User-Agent", "YaNeoDex-Desktop-OCR/1.0")
            setRequestProperty("Accept", "application/json")
            authToken.takeIf { it.isNotBlank() }?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
    }

    private fun readOrThrow(connection: HttpURLConnection, prefix: String): String {
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream ?: connection.inputStream
        val payload = stream.use { it.bufferedReader().readText() }
        if (connection.responseCode !in 200..299) {
            error("$prefix HTTP ${connection.responseCode}: ${payload.take(180)}")
        }
        return payload
    }

    private fun normalizeServerUrl(value: String): String {
        val normalized = value.trim().trimEnd('/')
        require(normalized.startsWith("http://") || normalized.startsWith("https://")) { "OCR server URL must start with http:// or https://." }
        return normalized
    }
}

@Serializable
private data class OcrCreateJobResponse(
    @SerialName("jobId") val jobId: String,
) {
    fun toResult(): OcrCreateJobResult = OcrCreateJobResult(jobId = jobId)
}

@Serializable
private data class OcrJobResponse(
    @SerialName("jobId") val jobId: String = "",
    val status: String = "",
    @SerialName("processedFiles") val processedFiles: Int = 0,
    @SerialName("totalFiles") val totalFiles: Int = 0,
    val candidates: List<RecognizedTrackCandidate> = emptyList(),
    val error: String? = null,
) {
    fun toResult(): OcrJobResult {
        return OcrJobResult(
            jobId = jobId,
            status = status,
            processedFiles = processedFiles,
            totalFiles = totalFiles,
            candidates = candidates,
            error = error,
        )
    }
}
