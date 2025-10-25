package org.cityxplore.backend.user.controller

import jakarta.validation.Valid
import org.cityxplore.backend.user.dto.UserCreateRequest
import org.cityxplore.backend.user.dto.UserResponse
import org.cityxplore.backend.user.service.UserService
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
 * Follows the standard conventions across the project: DTOs as API contract,
 * Bean Validation on input and Location header on creation.
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
    fun getAllUsers(): List<UserResponse> = userService.getAll()

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param id the unique identifier of the user to be retrieved
     * @return the user entity associated with the provided identifier
     * @throws java.util.NoSuchElementException if no user is found with the given identifier
     */
    @GetMapping("/{id}")
    fun getUser(@PathVariable id: UUID): UserResponse = userService.getById(id)

    /**
     * Creates a new user in the system.
     *
     * @param user the user data to be saved
     * @return the created user entity
     */
    @PostMapping
    fun createUser(@Valid @RequestBody user: UserCreateRequest): ResponseEntity<UserResponse> {
        val created = userService.create(user)
        val location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id)
            .toUri()

        return ResponseEntity.created(location).body(created)
    }
}
