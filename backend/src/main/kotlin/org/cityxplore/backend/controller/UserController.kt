package org.cityxplore.backend.controller

import org.cityxplore.backend.entity.User
import org.cityxplore.backend.repository.UserRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/users")
class UserController(private val userRepository: UserRepository) {

    @GetMapping
    fun getAllUsers(): List<User> = userRepository.findAll()

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: String): User = userRepository.findById(UUID.fromString(id)).orElseThrow()

    @PostMapping
    fun createUser(@RequestBody user: User): User = userRepository.save(user)
}