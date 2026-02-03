package app.cityxplore.social.data.repository

import app.cityxplore.social.data.remote.dto.CustomPoiDataDto
import app.cityxplore.social.data.remote.dto.SharePoiRequestDto
import app.cityxplore.social.data.remote.dto.SharedPoiResponseDto
import app.cityxplore.social.domain.model.SharePoiRequest
import app.cityxplore.social.domain.model.SharedPoi
import app.cityxplore.social.domain.repository.SharedPoiRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Implementation of [SharedPoiRepository] using Ktor for remote API calls.
 * Uses StateFlow for caching data following the offline-first pattern.
 */
class SharedPoiRepositoryImpl(
    private val client: HttpClient
) : SharedPoiRepository {

    companion object {
        private const val BASE_URL = "https://api.cityxplore.app/api/shared-pois"
    }

    // Internal cache using StateFlows
    private val _sentPois = MutableStateFlow<List<SharedPoi>>(emptyList())
    private val _receivedPois = MutableStateFlow<List<SharedPoi>>(emptyList())
    private val _unviewedPois = MutableStateFlow<List<SharedPoi>>(emptyList())

    // Read operations

    override fun getSentPois(): Flow<List<SharedPoi>> = _sentPois.asStateFlow()

    override fun getReceivedPois(): Flow<List<SharedPoi>> = _receivedPois.asStateFlow()

    override fun getUnviewedPois(): Flow<List<SharedPoi>> = _unviewedPois.asStateFlow()

    override fun getUnviewedCount(): Flow<Int> = _unviewedPois.map { it.size }

    // Refresh operations

    override suspend fun refreshSentPois(): Result<Unit> = runCatching {
        val response = client.get("$BASE_URL/sent").body<List<SharedPoiResponseDto>>()
        _sentPois.update { response.map { it.toDomain() } }
    }

    override suspend fun refreshReceivedPois(): Result<Unit> = runCatching {
        val response = client.get("$BASE_URL/received").body<List<SharedPoiResponseDto>>()
        _receivedPois.value = response.map { it.toDomain() }
    }

    override suspend fun refreshUnviewedPois(): Result<Unit> = runCatching {
        val response = client.get("$BASE_URL/received/unviewed").body<List<SharedPoiResponseDto>>()
        _unviewedPois.update { response.map { it.toDomain() } }
    }

    // Write operations

    override suspend fun sharePoi(request: SharePoiRequest): Result<SharedPoi> = runCatching {
        val requestDto = SharePoiRequestDto(
            recipientId = request.recipientId,
            poiId = request.poiId,
            customPoi = request.customPoi?.let { CustomPoiDataDto.fromDomain(it) },
            message = request.message
        )

        val response = client.post(BASE_URL) {
            contentType(ContentType.Application.Json)
            setBody(requestDto)
        }.body<SharedPoiResponseDto>()

        val sharedPoi = response.toDomain()

        // Update sent POIs cache
        _sentPois.update { current -> listOf(sharedPoi) + current }

        sharedPoi
    }

    override suspend fun markViewed(sharedPoiId: String): Result<SharedPoi> = runCatching {
        val response = client.post("$BASE_URL/$sharedPoiId/viewed")
            .body<SharedPoiResponseDto>()

        val updatedPoi = response.toDomain()

        // Update caches
        _receivedPois.update { current ->
            current.map { if (it.id == sharedPoiId) updatedPoi else it }
        }
        _unviewedPois.update { current ->
            current.filter { it.id != sharedPoiId }
        }

        updatedPoi
    }

    override suspend fun discoverSharedPoi(sharedPoiId: String): Result<SharedPoi> = runCatching {
        val response = client.post("$BASE_URL/$sharedPoiId/discover")
            .body<SharedPoiResponseDto>()

        val updatedPoi = response.toDomain()

        // Update received POIs cache
        _receivedPois.update { current ->
            current.map { if (it.id == sharedPoiId) updatedPoi else it }
        }

        updatedPoi
    }

    override suspend fun deleteSharedPoi(sharedPoiId: String): Result<Unit> = runCatching {
        client.delete("$BASE_URL/$sharedPoiId")

        // Remove from sent POIs cache
        _sentPois.update { current ->
            current.filter { it.id != sharedPoiId }
        }
    }

    override suspend fun getSharedPoiById(sharedPoiId: String): Result<SharedPoi> = runCatching {
        val response = client.get("$BASE_URL/$sharedPoiId")
            .body<SharedPoiResponseDto>()
        response.toDomain()
    }

    override suspend fun uploadPoiImage(imageBytes: ByteArray): Result<String> = runCatching {
        val contentType = detectMimeType(imageBytes)
            ?: throw IllegalArgumentException("Unsupported image format")
        val filename = "image.${contentType.contentSubtype}"

        val response = client.submitFormWithBinaryData(
            url = "$BASE_URL/images",
            formData = formData {
                append("file", imageBytes, Headers.build {
                    append(HttpHeaders.ContentType, contentType.toString())
                    append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                })
            }
        ).body<Map<String, String>>()

        response["url"] ?: throw IllegalStateException("Backend returned null URL")
    }

    private fun detectMimeType(bytes: ByteArray): ContentType? {
        // Simple magic bytes check
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        ) {
            return ContentType.Image.PNG
        }
        // JPEG: FF D8 FF
        if (bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
        ) {
            return ContentType.Image.JPEG
        }
        // WEBP: RIFF ... WEBP
        if (bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() && bytes[10] == 'B'.code.toByte() &&
            bytes[11] == 'P'.code.toByte()
        ) {
            return ContentType.Image.WEBP
        }
        return null
    }

    override fun clearCache() {
        _sentPois.value = emptyList()
        _receivedPois.value = emptyList()
        _unviewedPois.value = emptyList()
    }
}
