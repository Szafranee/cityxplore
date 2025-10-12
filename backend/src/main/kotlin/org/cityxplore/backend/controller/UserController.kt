package org.cityxplore.backend.controller

import jakarta.validation.Valid
import org.cityxplore.backend.dto.UserCreateRequest
import org.cityxplore.backend.dto.UserResponseDto
import org.cityxplore.backend.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

/**
 * REST controller for managing user-related operations.
 *
 * Handles HTTP requests related to the `User` entity, including retrieving all users,
 * retrieving a specific user by ID, and creating a new user.
 *
 * This controller is mapped to the `/api/users` endpoint.
 *
 * @constructor Initializes the controller with a `UserService` instance for user-related operations.
 */
@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

    /**
     * Retrieves all users from the database.
     *
     * @return a list of all users in the system.
     */
    @GetMapping
    fun getAllUsers(): List<UserResponseDto> = userService.getAll()

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param id the unique identifier of the user to be retrieved
     * @return the user entity associated with the provided identifier
     * @throws java.util.NoSuchElementException if no user is found with the given identifier
     */
    @GetMapping("/{id}")
    fun getUser(@PathVariable id: UUID): UserResponseDto = userService.getById(id)

    /**
     * Creates a new user in the system.
     *
     * @param user the user data to be saved
     * @return the created user entity
     */
    @PostMapping
    fun createUser(@Valid @RequestBody user: UserCreateRequest): ResponseEntity<UserResponseDto> {
        val created = userService.create(user)
        val location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id)
            .toUri()

        return ResponseEntity.created(location).body(created)
    }
}