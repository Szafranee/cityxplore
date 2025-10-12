package org.cityxplore.backend.service

import org.cityxplore.backend.dto.UserCreateRequest
import org.cityxplore.backend.dto.UserResponseDto
import org.cityxplore.backend.mapper.toEntity
import org.cityxplore.backend.mapper.toUserResponseDto
import org.cityxplore.backend.mapper.toUserResponseDtoList
import org.cityxplore.backend.repository.UserRepository
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
     * Fetches all users from the repository and maps them to a list of response DTOs.
     *
     * @return A list of `UserResponseDto` objects representing all users.
     */
    @Transactional(readOnly = true)
    fun getAll(): List<UserResponseDto> =
        userRepository.findAll().toUserResponseDtoList()

    /**
     * Retrieves a user by their unique identifier.
     * Throws a `ResponseStatusException` with a 404 status if the user is not found.
     *
     * @param id The unique identifier of the user to retrieve.
     * @return A `UserResponseDto` object representing the retrieved user.
     */
    @Transactional(readOnly = true)
    fun getById(id: UUID): UserResponseDto =
        userRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
            .toUserResponseDto()

    /**
     * Creates a new user based on the provided request data.
     *
     * @param request The user creation request containing the necessary details
     *                including email, username, and optional avatar URL.
     * @return A `UserResponseDto` representing the created user, containing details
     *         such as the user ID, email, username, avatar URL, creation timestamp,
     *         last active timestamp, total distance traveled, and total Points of Interest (POIs) discovered.
     */
    @Transactional
    fun create(request: UserCreateRequest): UserResponseDto =
        userRepository.save(request.toEntity()).toUserResponseDto()
}

