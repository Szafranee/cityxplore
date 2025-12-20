package org.cityxplore.backend.user.repository

import org.cityxplore.backend.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

/**
 * Repository interface for performing CRUD operations on User entities, implemented by Spring Data JPA.
 * Provides additional methods to query and modify user data.
 */
interface UserRepository : JpaRepository<User, UUID> {
    /**
     * Retrieves a User entity by its username.
     *
     * @param username the username of the user to be retrieved
     * @return the User entity if found, or null if no user exists with the given username
     */
    fun findByUsername(username: String): User?

    /**
     * Retrieves a User entity by its email.
     *
     * @param email the email of the user to be retrieved
     * @return the User entity if found, or null if no user exists with the given email
     */
    fun findByEmail(email: String): User?

    /**
     * Counts the number of users with an active status in the system.
     *
     * @return the number of users where the isActive field is true
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    fun countByIsActiveTrue(): Long

    /**
     * Increments the total achievement points of a user specified by their ID.
     *
     * @param userId The unique identifier of the user whose achievement points need to be updated.
     * @param points The number of achievement points to be added to the user's total.
     * @return The number of rows affected by the update operation.
     */
    @Modifying
    @Query("UPDATE User u SET u.totalAchievementPoints = u.totalAchievementPoints + :points WHERE u.id = :userId")
    fun incrementAchievementPoints(userId: UUID, points: Int): Int

    /**
     * Increments the total number of POIs (Points of Interest) discovered by the user by 1.
     *
     * @param userId the unique identifier of the user whose total POIs discovered is to be incremented
     * @return the number of rows affected by the update operation
     */
    @Modifying
    @Query("UPDATE User u SET u.totalPoisDiscovered = u.totalPoisDiscovered + 1 WHERE u.id = :userId")
    fun incrementPoisDiscovered(userId: UUID): Int
}
