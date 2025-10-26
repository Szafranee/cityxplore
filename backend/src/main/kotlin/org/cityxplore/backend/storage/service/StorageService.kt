package org.cityxplore.backend.storage.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder
import java.time.Duration

/**
 * Service for interacting with Supabase Storage over REST.
 */
@Service
class StorageService(
    @Value("\${supabase.storage.url}") private val storageUrl: String,
    @Value("\${supabase.secret-key}") private val serviceKey: String
) {
    private val restTemplate: RestTemplate = RestTemplate().apply {
        (requestFactory as? SimpleClientHttpRequestFactory)?.let {
            it.setConnectTimeout(Duration.ofSeconds(3))
            it.setReadTimeout(Duration.ofSeconds(5))
        }
    }

    fun createSignedUrl(bucket: String, path: String, expiresIn: Int = 3600): String {
        val url = UriComponentsBuilder.fromUriString(storageUrl)
            .pathSegment("object", "sign", bucket)
            .pathSegment(*path.trimStart('/').split('/').toTypedArray())
            .build()
            .toUriString()
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set(HttpHeaders.AUTHORIZATION, "Bearer $serviceKey")
            set("apikey", serviceKey)
        }
        val body = mapOf("expiresIn" to expiresIn)
        val entity = HttpEntity(body, headers)
        val response = restTemplate.postForEntity(url, entity, Map::class.java)

        val signedFragment = response.body?.get("signedURL")?.toString()
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing signedURL in response")

        // Supabase returns a path fragment; compose absolute URL
        val absolute = if (signedFragment.startsWith("http", ignoreCase = true)) {
            signedFragment
        } else {
            UriComponentsBuilder.fromUriString(storageUrl)
                .path(signedFragment)
                .build()
                .toUriString()
        }

        return absolute
    }

    fun deleteFile(bucket: String, path: String) {
        val url = UriComponentsBuilder.fromUriString(storageUrl)
            .pathSegment("object", bucket)
            .pathSegment(*path.trimStart('/').split('/').toTypedArray())
            .build()
            .toUriString()
        val headers = HttpHeaders().apply {
            set(HttpHeaders.AUTHORIZATION, "Bearer $serviceKey")
            set("apikey", serviceKey)
        }
        val entity = HttpEntity<Void>(headers)
        try {
            val response = restTemplate.exchange(
                url, HttpMethod.DELETE, entity, String::class.java
            )

            if (!response.statusCode.is2xxSuccessful) {
                throw ResponseStatusException(response.statusCode, "Failed to delete file")
            }
        } catch (_: HttpClientErrorException.NotFound) {
            // treat as success
            return
        } catch (e: RestClientResponseException) {
            throw ResponseStatusException(e.statusCode, "Failed to delete file", e)
        }
    }
}
