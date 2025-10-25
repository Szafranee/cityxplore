package org.cityxplore.backend.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class StorageService(
    @Value("\${supabase.storage.url}") private val storageUrl: String,
    @Value("\${supabase.secret-key}") private val serviceKey: String
) {

    private val restTemplate = RestTemplate()

    fun createSignedUrl(bucket: String, path: String, expiresIn: Int = 3600): String {
        val url = "$storageUrl/object/sign/$bucket/$path"
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("Authorization", "Bearer $serviceKey")
            set("apikey", serviceKey)
        }
        val body = mapOf("expiresIn" to expiresIn)
        val entity = HttpEntity(body, headers)
        val response = restTemplate.postForEntity(url, entity, Map::class.java)

        return storageUrl + (response.body?.get("signedURL")?.toString() ?: "")
    }

    fun deleteFile(bucket: String, path: String) {
        val url = "$storageUrl/object/$bucket/$path"
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $serviceKey")
            set("apikey", serviceKey)
        }
        val entity = HttpEntity<Void>(headers)
        restTemplate.exchange(url, HttpMethod.DELETE, entity, String::class.java)
    }
}
