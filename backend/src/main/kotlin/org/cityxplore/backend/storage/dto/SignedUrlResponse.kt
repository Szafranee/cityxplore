package org.cityxplore.backend.storage.dto

/**
 * Represents the response containing a signed URL for accessing or interacting with
 * files in Supabase storage.
 *
 * This data class holds the signed URL returned from the server,
 * which can be used to perform actions like file retrieval or manipulation
 * in a secure and time-limited manner.
 *
 * @property signedURL The signed URL provided by Supabase. Can be null if the generation fails.
 */
data class SignedUrlResponse(val signedURL: String?)
