package org.cityxplore.backend.user.service

import org.cityxplore.backend.user.dto.UserCreateRequest
import org.cityxplore.backend.user.dto.UserResponse
import org.cityxplore.backend.user.mapper.toEntity
import org.cityxplore.backend.user.mapper.toUserResponse
import org.cityxplore.backend.user.mapper.toUserResponseList
import org.cityxplore.backend.user.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * Service class for managing User-related operations such as retrieving all users, retrieving a user
 * by its unique identifier, and creating a new user. This class interacts with the `UserRepository`
 * for persistence and uses mapping utilities to transform entities into DTOs.
 *
 * @property userRepository Repository used to interact with the persistence layer for User data.
 */
@Service
class UserService(
    private val userRepository: UserRepository
) {

    /**
     * Fetches all ACTIVE users from the repository and maps them to a list of response DTOs.
     * Soft-deleted users are excluded.
     *
     * @return A list of `UserResponse` objects representing all active users.
     */
    @Transactional(readOnly = true)
    fun getAll(): List<UserResponse> =
        userRepository.findAll().filter { it.isActive }.toUserResponseList()

    /**
     * Retrieves a user by their unique identifier.
     * Throws a `ResponseStatusException` with a 404 status if the user is not found or inactive.
     *
     * @param id The unique identifier of the user to retrieve.
     * @return A `UserResponse` object representing the retrieved user.
     */
    @Transactional(readOnly = true)
    fun getById(id: UUID): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        if (!user.isActive) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }

        return user.toUserResponse()
    }

    /**
     * Creates a new user based on the provided request data.
     *
     * @param userCreateRequest The user creation request containing the necessary details
     *                including email, username, and optional avatar URL.
     * @return A `UserResponse` representing the created user, containing details
     *         such as the user ID, email, username, avatar URL, creation timestamp,
     *         last active timestamp, total distance travelled, and total Points of Interest (POIs) discovered.
     */
    @Transactional
    fun create(userCreateRequest: UserCreateRequest, id: UUID? = null): UserResponse {
        val existing = userRepository.findByEmail(userCreateRequest.email)
        if (existing != null) {
            if (id != null && existing.id != id) {
                userRepository.delete(existing)
                userRepository.flush()
            } else {
                throw ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists")
            }
        }

        if (userRepository.findByUsername(userCreateRequest.username) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken")
        }

        return try {
            userRepository.save(userCreateRequest.toEntity(id)).toUserResponse()
        } catch (_: DataIntegrityViolationException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "User already exists or username taken")
        }
    }
}
