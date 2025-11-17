package org.cityxplore.backend.poi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.cityxplore.backend.config.JpaAuditingConfiguration
import org.cityxplore.backend.poi.dto.CreatePoiRequest
import org.cityxplore.backend.poi.dto.PoiAdminResponse
import org.cityxplore.backend.poi.dto.UpdatePoiRequest
import org.cityxplore.backend.poi.service.PointOfInterestService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(
    controllers = [PoiAdminController::class],
    excludeFilters = [ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = [JpaAuditingConfiguration::class]
    )]
)
@ImportAutoConfiguration(exclude = [DataSourceAutoConfiguration::class, HibernateJpaAutoConfiguration::class])
@EnableMethodSecurity
class PoiAdminControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var poiService: PointOfInterestService

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `getAll should return all POIs including inactive`() {
        // Given
        val activePoi = PoiAdminResponse(
            id = UUID.randomUUID(),
            name = "Active Museum",
            description = "Active museum",
            category = "Culture",
            latitude = 52.0,
            longitude = 21.0,
            metadata = null,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val inactivePoi = PoiAdminResponse(
            id = UUID.randomUUID(),
            name = "Inactive Museum",
            description = "Inactive museum",
            category = "Culture",
            latitude = 52.1,
            longitude = 21.1,
            metadata = null,
            isActive = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { poiService.getAllPois() } returns listOf(activePoi, inactivePoi)

        // When & Then
        mockMvc.perform(get("/api/admin/pois"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Active Museum"))
            .andExpect(jsonPath("$[0].isActive").value(true))
            .andExpect(jsonPath("$[1].name").value("Inactive Museum"))
            .andExpect(jsonPath("$[1].isActive").value(false))

        verify(exactly = 1) { poiService.getAllPois() }
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `getAll should return 403 for non-admin user`() {
        // When & Then
        mockMvc.perform(get("/api/admin/pois"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Access Denied"))

        verify(exactly = 0) { poiService.getAllPois() }
    }

    @Test
    fun `getAll should return 401 when unauthorized`() {
        // When & Then
        mockMvc.perform(get("/api/admin/pois"))
            .andExpect(status().isUnauthorized)

        verify(exactly = 0) { poiService.getAllPois() }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `getById should return POI when found`() {
        // Given
        val poiId = UUID.randomUUID()
        val poi = PoiAdminResponse(
            id = poiId,
            name = "Museum",
            description = "Historic museum",
            category = "Culture",
            latitude = 52.0,
            longitude = 21.0,
            metadata = mapOf("key" to "value"),
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { poiService.getPoiById(poiId) } returns poi

        // When & Then
        mockMvc.perform(get("/api/admin/pois/{id}", poiId))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(poiId.toString()))
            .andExpect(jsonPath("$.name").value("Museum"))
            .andExpect(jsonPath("$.category").value("Culture"))
            .andExpect(jsonPath("$.isActive").value(true))

        verify(exactly = 1) { poiService.getPoiById(poiId) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `getById should return 404 when POI not found`() {
        // Given
        val poiId = UUID.randomUUID()
        every { poiService.getPoiById(poiId) } throws ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found")

        // When & Then
        mockMvc.perform(get("/api/admin/pois/{id}", poiId))
            .andExpect(status().isNotFound)

        verify(exactly = 1) { poiService.getPoiById(poiId) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `createPoi should create POI and return 201 with Location header`() {
        // Given
        val request = CreatePoiRequest(
            name = "New Museum",
            description = "Brand new museum",
            category = "Culture",
            latitude = 52.0,
            longitude = 21.0,
            metadata = mapOf("opening_hours" to "9-17")
        )

        val createdPoi = PoiAdminResponse(
            id = UUID.randomUUID(),
            name = request.name,
            description = request.description,
            category = request.category,
            latitude = request.latitude,
            longitude = request.longitude,
            metadata = request.metadata,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { poiService.createPoi(any()) } returns createdPoi

        // When & Then
        mockMvc.perform(
            post("/api/admin/pois")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/admin/pois/")))
            .andExpect(jsonPath("$.name").value("New Museum"))
            .andExpect(jsonPath("$.category").value("Culture"))
            .andExpect(jsonPath("$.latitude").value(52.0))
            .andExpect(jsonPath("$.isActive").value(true))

        verify(exactly = 1) { poiService.createPoi(any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `createPoi should return 400 for blank name`() {
        // Given
        val invalidRequest = CreatePoiRequest(
            name = "",
            description = "Description",
            category = "Culture",
            latitude = 52.0,
            longitude = 21.0,
            metadata = null
        )

        // When & Then
        mockMvc.perform(
            post("/api/admin/pois")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { poiService.createPoi(any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `createPoi should return 400 for invalid coordinates`() {
        // Given
        val request = CreatePoiRequest(
            name = "Invalid POI",
            description = "Description",
            category = "Culture",
            latitude = 100.0, // Invalid: > 90 - Bean Validation catches this
            longitude = 21.0,
            metadata = null
        )

        // When & Then
        mockMvc.perform(
            post("/api/admin/pois")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.fieldErrors.latitude").exists())

        // Bean Validation prevents service call
        verify(exactly = 0) { poiService.createPoi(any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `updatePoi should update existing POI`() {
        // Given
        val poiId = UUID.randomUUID()
        val updateRequest = UpdatePoiRequest(
            name = "Updated Museum",
            description = "Updated description",
            category = "UpdatedCategory",
            latitude = 52.5,
            longitude = 21.5,
            metadata = mapOf("updated" to "true")
        )

        val updatedPoi = PoiAdminResponse(
            id = poiId,
            name = updateRequest.name,
            description = updateRequest.description,
            category = updateRequest.category,
            latitude = updateRequest.latitude,
            longitude = updateRequest.longitude,
            metadata = updateRequest.metadata,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { poiService.updatePoi(poiId, any()) } returns updatedPoi

        // When & Then
        mockMvc.perform(
            put("/api/admin/pois/{id}", poiId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(poiId.toString()))
            .andExpect(jsonPath("$.name").value("Updated Museum"))
            .andExpect(jsonPath("$.category").value("UpdatedCategory"))
            .andExpect(jsonPath("$.latitude").value(52.5))

        verify(exactly = 1) { poiService.updatePoi(poiId, any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `updatePoi should return 404 when POI not found`() {
        // Given
        val poiId = UUID.randomUUID()
        val updateRequest = UpdatePoiRequest(
            name = "Updated",
            description = "Updated",
            category = "Updated",
            latitude = 52.0,
            longitude = 21.0,
            metadata = null
        )

        every { poiService.updatePoi(poiId, any()) } throws ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "POI not found"
        )

        // When & Then
        mockMvc.perform(
            put("/api/admin/pois/{id}", poiId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isNotFound)

        verify(exactly = 1) { poiService.updatePoi(poiId, any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `updatePoi should return 400 for blank name`() {
        // Given
        val poiId = UUID.randomUUID()
        val invalidRequest = UpdatePoiRequest(
            name = "",
            description = "Description",
            category = "Culture",
            latitude = 52.0,
            longitude = 21.0,
            metadata = null
        )

        // When & Then
        mockMvc.perform(
            put("/api/admin/pois/{id}", poiId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { poiService.updatePoi(any(), any()) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `deletePoi should delete POI and return 204`() {
        // Given
        val poiId = UUID.randomUUID()
        every { poiService.deletePoi(poiId) } just Runs

        // When & Then
        mockMvc.perform(
            delete("/api/admin/pois/{id}", poiId)
                .with(csrf())
        )
            .andExpect(status().isNoContent)

        verify(exactly = 1) { poiService.deletePoi(poiId) }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `deletePoi should return 404 when POI not found`() {
        // Given
        val poiId = UUID.randomUUID()
        every { poiService.deletePoi(poiId) } throws ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found")

        // When & Then
        mockMvc.perform(
            delete("/api/admin/pois/{id}", poiId)
                .with(csrf())
        )
            .andExpect(status().isNotFound)

        verify(exactly = 1) { poiService.deletePoi(poiId) }
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `deletePoi should return 403 for non-admin user`() {
        // Given
        val poiId = UUID.randomUUID()

        // When & Then
        mockMvc.perform(
            delete("/api/admin/pois/{id}", poiId)
                .with(csrf())
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Access Denied"))

        verify(exactly = 0) { poiService.deletePoi(any()) }
    }
}
