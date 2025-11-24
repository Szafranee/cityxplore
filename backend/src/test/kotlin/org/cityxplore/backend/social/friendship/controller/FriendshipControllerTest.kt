package org.cityxplore.backend.social.friendship.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.cityxplore.backend.social.friendship.dto.FriendshipRequest
import org.cityxplore.backend.social.friendship.dto.FriendshipResponse
import org.cityxplore.backend.social.friendship.entity.FriendshipStatus
import org.cityxplore.backend.social.friendship.service.FriendshipService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

/**
 * Integration tests for FriendshipController.
 */
@WebMvcTest(FriendshipController::class)
class FriendshipControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var friendshipService: FriendshipService

    /**
     * Creates a JWT mock with a valid UUID as the subject claim.
     * This is needed because JwtUtils.extractUserId expects a UUID in the 'sub' claim.
     */
    private fun jwtWithSubject(userId: UUID = UUID.randomUUID()) = jwt().jwt { builder ->
        builder.subject(userId.toString())
    }

    // ========== sendInvite Tests ==========

    @Test
    @WithMockUser(username = "user")
    fun `sendInvite should return 201 when friendship invitation is created`() {
        // Given
        val requesterId = UUID.randomUUID()
        val addresseeId = UUID.randomUUID()
        val friendshipRequest = FriendshipRequest(addresseeId)
        val friendshipId = UUID.randomUUID()

        val response = FriendshipResponse(
            id = friendshipId,
            requesterId = requesterId,
            addresseeId = addresseeId,
            status = FriendshipStatus.PENDING,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { friendshipService.sendInvite(any(), any()) } returns response

        // When & Then
        mockMvc.perform(
            post("/api/friends/invite")
                .with(csrf())
                .with(jwtWithSubject())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(friendshipRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").value(friendshipId.toString()))
            .andExpect(jsonPath("$.status").value("PENDING"))

        verify(exactly = 1) { friendshipService.sendInvite(any(), any()) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `sendInvite should return 400 when trying to invite yourself`() {
        // Given
        val userId = UUID.randomUUID()
        val friendshipRequest = FriendshipRequest(userId)

        every { friendshipService.sendInvite(any(), any()) } throws ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Cannot invite yourself"
        )

        // When & Then
        mockMvc.perform(
            post("/api/friends/invite")
                .with(csrf())
                .with(jwtWithSubject())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(friendshipRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Cannot invite yourself"))
    }

    @Test
    @WithMockUser(username = "user")
    fun `sendInvite should return 409 when pending request already exists`() {
        // Given
        val addresseeId = UUID.randomUUID()
        val friendshipRequest = FriendshipRequest(addresseeId)

        every { friendshipService.sendInvite(any(), any()) } throws ResponseStatusException(
            HttpStatus.CONFLICT,
            "A friendship request is already pending"
        )

        // When & Then
        mockMvc.perform(
            post("/api/friends/invite")
                .with(csrf())
                .with(jwtWithSubject())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(friendshipRequest))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("A friendship request is already pending"))
    }

    @Test
    @WithMockUser(username = "user")
    fun `sendInvite should return 409 when users are already friends`() {
        // Given
        val addresseeId = UUID.randomUUID()
        val friendshipRequest = FriendshipRequest(addresseeId)

        every { friendshipService.sendInvite(any(), any()) } throws ResponseStatusException(
            HttpStatus.CONFLICT,
            "You are already friends with this user"
        )

        // When & Then
        mockMvc.perform(
            post("/api/friends/invite")
                .with(csrf())
                .with(jwtWithSubject())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(friendshipRequest))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("You are already friends with this user"))
    }

    @Test
    @WithMockUser(username = "user")
    fun `sendInvite should return 403 when user is blocked`() {
        // Given
        val addresseeId = UUID.randomUUID()
        val friendshipRequest = FriendshipRequest(addresseeId)

        every { friendshipService.sendInvite(any(), any()) } throws ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Cannot send invitation to this user"
        )

        // When & Then
        mockMvc.perform(
            post("/api/friends/invite")
                .with(csrf())
                .with(jwtWithSubject())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(friendshipRequest))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Cannot send invitation to this user"))
    }

    @Test
    fun `sendInvite should return 401 when user is not authenticated`() {
        // Given
        val addresseeId = UUID.randomUUID()
        val friendshipRequest = FriendshipRequest(addresseeId)

        // When & Then
        mockMvc.perform(
            post("/api/friends/invite")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(friendshipRequest))
        )
            .andExpect(status().isUnauthorized)
    }

    // ========== acceptInvite Tests ==========

    @Test
    @WithMockUser(username = "user")
    fun `acceptInvite should return 200 when friendship is accepted`() {
        // Given
        val currentUserId = UUID.randomUUID()
        val requesterId = UUID.randomUUID()
        val friendshipId = UUID.randomUUID()

        val response = FriendshipResponse(
            id = friendshipId,
            requesterId = requesterId,
            addresseeId = currentUserId,
            status = FriendshipStatus.ACCEPTED,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { friendshipService.acceptInvite(any(), friendshipId) } returns response

        // When & Then
        mockMvc.perform(
            post("/api/friends/{friendshipId}/accept", friendshipId)
                .with(csrf())
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(friendshipId.toString()))
            .andExpect(jsonPath("$.status").value("ACCEPTED"))

        verify(exactly = 1) { friendshipService.acceptInvite(any(), friendshipId) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `acceptInvite should return 404 when friendship not found`() {
        // Given
        val friendshipId = UUID.randomUUID()

        every { friendshipService.acceptInvite(any(), friendshipId) } throws ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Friend request not found"
        )

        // When & Then
        mockMvc.perform(
            post("/api/friends/{friendshipId}/accept", friendshipId)
                .with(csrf())
                .with(jwtWithSubject())
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Friend request not found"))
    }

    @Test
    @WithMockUser(username = "user")
    fun `acceptInvite should return 403 when not addressee`() {
        // Given
        val friendshipId = UUID.randomUUID()

        every { friendshipService.acceptInvite(any(), friendshipId) } throws ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Not your invitation to accept"
        )

        // When & Then
        mockMvc.perform(
            post("/api/friends/{friendshipId}/accept", friendshipId)
                .with(csrf())
                .with(jwtWithSubject())
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Not your invitation to accept"))
    }

    @Test
    @WithMockUser(username = "user")
    fun `acceptInvite should return 409 when invitation is not pending`() {
        // Given
        val friendshipId = UUID.randomUUID()

        every { friendshipService.acceptInvite(any(), friendshipId) } throws ResponseStatusException(
            HttpStatus.CONFLICT,
            "Invitation is not pending"
        )

        // When & Then
        mockMvc.perform(
            post("/api/friends/{friendshipId}/accept", friendshipId)
                .with(csrf())
                .with(jwtWithSubject())
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Invitation is not pending"))
    }

    // ========== declineInvite Tests ==========

    @Test
    @WithMockUser(username = "user")
    fun `declineInvite should return 200 when friendship is declined`() {
        // Given
        val currentUserId = UUID.randomUUID()
        val requesterId = UUID.randomUUID()
        val friendshipId = UUID.randomUUID()

        val response = FriendshipResponse(
            id = friendshipId,
            requesterId = requesterId,
            addresseeId = currentUserId,
            status = FriendshipStatus.DECLINED,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { friendshipService.declineInvite(any(), friendshipId) } returns response

        // When & Then
        mockMvc.perform(
            post("/api/friends/{friendshipId}/decline", friendshipId)
                .with(csrf())
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(friendshipId.toString()))
            .andExpect(jsonPath("$.status").value("DECLINED"))

        verify(exactly = 1) { friendshipService.declineInvite(any(), friendshipId) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `declineInvite should return 404 when friendship not found`() {
        // Given
        val friendshipId = UUID.randomUUID()

        every { friendshipService.declineInvite(any(), friendshipId) } throws ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Friend request not found"
        )

        // When & Then
        mockMvc.perform(
            post("/api/friends/{friendshipId}/decline", friendshipId)
                .with(csrf())
                .with(jwtWithSubject())
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Friend request not found"))
    }

    @Test
    @WithMockUser(username = "user")
    fun `declineInvite should return 403 when not addressee`() {
        // Given
        val friendshipId = UUID.randomUUID()

        every { friendshipService.declineInvite(any(), friendshipId) } throws ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Not your invitation to decline"
        )

        // When & Then
        mockMvc.perform(
            post("/api/friends/{friendshipId}/decline", friendshipId)
                .with(csrf())
                .with(jwtWithSubject())
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Not your invitation to decline"))
    }

    // ========== getFriendship Tests ==========

    @Test
    @WithMockUser(username = "user")
    fun `getFriendship should return 200 with friendship details`() {
        // Given
        val userId = UUID.randomUUID()
        val friendId = UUID.randomUUID()
        val friendshipId = UUID.randomUUID()

        val response = FriendshipResponse(
            id = friendshipId,
            requesterId = userId,
            addresseeId = friendId,
            status = FriendshipStatus.ACCEPTED,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { friendshipService.getFriendshipById(any(), friendshipId) } returns response

        // When & Then
        mockMvc.perform(
            get("/api/friends/{friendshipId}", friendshipId)
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(friendshipId.toString()))
            .andExpect(jsonPath("$.status").value("ACCEPTED"))

        verify(exactly = 1) { friendshipService.getFriendshipById(any(), friendshipId) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `getFriendship should return 404 when friendship not found`() {
        // Given
        val friendshipId = UUID.randomUUID()

        every { friendshipService.getFriendshipById(any(), friendshipId) } throws ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Friendship not found"
        )

        // When & Then
        mockMvc.perform(
            get("/api/friends/{friendshipId}", friendshipId)
                .with(jwtWithSubject())
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Friendship not found"))
    }

    @Test
    @WithMockUser(username = "user")
    fun `getFriendship should return 403 when user not part of friendship`() {
        // Given
        val friendshipId = UUID.randomUUID()

        every { friendshipService.getFriendshipById(any(), friendshipId) } throws ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "You do not have access to this friendship"
        )

        // When & Then
        mockMvc.perform(
            get("/api/friends/{friendshipId}", friendshipId)
                .with(jwtWithSubject())
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("You do not have access to this friendship"))
    }

    // ========== getMyFriends Tests ==========

    @Test
    @WithMockUser(username = "user")
    fun `getMyFriends should return 200 with list of friends`() {
        // Given
        val userId = UUID.randomUUID()
        val friend1Id = UUID.randomUUID()
        val friend2Id = UUID.randomUUID()

        val friends = listOf(
            FriendshipResponse(
                id = UUID.randomUUID(),
                requesterId = userId,
                addresseeId = friend1Id,
                status = FriendshipStatus.ACCEPTED,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            ),
            FriendshipResponse(
                id = UUID.randomUUID(),
                requesterId = friend2Id,
                addresseeId = userId,
                status = FriendshipStatus.ACCEPTED,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        every { friendshipService.getMyFriends(any()) } returns friends

        // When & Then
        mockMvc.perform(
            get("/api/friends")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].status").value("ACCEPTED"))
            .andExpect(jsonPath("$[1].status").value("ACCEPTED"))

        verify(exactly = 1) { friendshipService.getMyFriends(any()) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `getMyFriends should return 200 with empty list when no friends`() {
        // Given
        every { friendshipService.getMyFriends(any()) } returns emptyList()

        // When & Then
        mockMvc.perform(
            get("/api/friends")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))

        verify(exactly = 1) { friendshipService.getMyFriends(any()) }
    }

    // ========== getPendingRequests Tests ==========

    @Test
    @WithMockUser(username = "user")
    fun `getPendingRequests should return 200 with list of pending invitations`() {
        // Given
        val userId = UUID.randomUUID()
        val requester1Id = UUID.randomUUID()
        val requester2Id = UUID.randomUUID()

        val pendingInvites = listOf(
            FriendshipResponse(
                id = UUID.randomUUID(),
                requesterId = requester1Id,
                addresseeId = userId,
                status = FriendshipStatus.PENDING,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            ),
            FriendshipResponse(
                id = UUID.randomUUID(),
                requesterId = requester2Id,
                addresseeId = userId,
                status = FriendshipStatus.PENDING,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        every { friendshipService.getPendingInvites(any()) } returns pendingInvites

        // When & Then
        mockMvc.perform(
            get("/api/friends/pending")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].status").value("PENDING"))
            .andExpect(jsonPath("$[1].status").value("PENDING"))

        verify(exactly = 1) { friendshipService.getPendingInvites(any()) }
    }

    @Test
    @WithMockUser(username = "user")
    fun `getPendingRequests should return 200 with empty list when no pending invitations`() {
        // Given
        every { friendshipService.getPendingInvites(any()) } returns emptyList()

        // When & Then
        mockMvc.perform(
            get("/api/friends/pending")
                .with(jwtWithSubject())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))

        verify(exactly = 1) { friendshipService.getPendingInvites(any()) }
    }

    @Test
    fun `getPendingRequests should return 401 when user is not authenticated`() {
        // When & Then
        mockMvc.perform(
            get("/api/friends/pending")
        )
            .andExpect(status().isUnauthorized)
    }
}
