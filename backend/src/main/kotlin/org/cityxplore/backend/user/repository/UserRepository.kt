package org.cityxplore.backend.user.repository

import org.cityxplore.backend.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByUsername(username: String): User?

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    fun countByIsActiveTrue(): Long
}
