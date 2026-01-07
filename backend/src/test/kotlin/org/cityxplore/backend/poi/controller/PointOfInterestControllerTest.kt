package org.cityxplore.backend.poi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.cityxplore.backend.config.JpaAuditingConfiguration
import org.cityxplore.backend.poi.dto.CreatePoiPublicRequest
import org.cityxplore.backend.poi.dto.PoiResponse
import org.cityxplore.backend.poi.entity.PoiImage
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
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(
    controllers = [PointOfInterestController::class],
    excludeFilters = [ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = [JpaAuditingConfiguration::class]
    )]
)
@ImportAutoConfiguration(exclude = [DataSourceAutoConfiguration::class, HibernateJpaAutoConfiguration::class])
class PointOfInterestControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var poiService: PointOfInterestService

    @Test
    @WithMockUser
    fun `getAllPOIs should return list of POIs`() {
        // Given
        val poi1 = PoiResponse(
            id = UUID.randomUUID(),
            name = "Museum",
            description = "Historic museum",
            category = "Culture",
            latitude = 52.0,
            longitude = 21.0,
            metadata = mapOf("opening_hours" to "9-17"),
            imageUrls = listOf(PoiImage(url = "https://example.com/img1.jpg")),
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val poi2 = PoiResponse(
            id = UUID.randomUUID(),
            name = "Park",
            description = "City park",
            category = "Nature",
            latitude = 52.1,
            longitude = 21.1,
            metadata = null,
            imageUrls = null,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { poiService.getAll() } returns listOf(poi1, poi2)

        // When & Then
        mockMvc.perform(get("/api/pois"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Museum"))
            .andExpect(jsonPath("$[0].category").value("Culture"))
            .andExpect(jsonPath("$[1].name").value("Park"))
            .andExpect(jsonPath("$[1].category").value("Nature"))

        verify(exactly = 1) { poiService.getAll() }
    }

    @Test
    @WithMockUser
    fun `getAllPOIs should return empty list when no POIs`() {
        // Given
        every { poiService.getAll() } returns emptyList()

        // When & Then
        mockMvc.perform(get("/api/pois"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(0))

        verify(exactly = 1) { poiService.getAll() }
    }

    @Test
    @WithMockUser
    fun `getPOI should return POI when found`() {
        // Given
        val poiId = UUID.randomUUID()
        val poi = PoiResponse(
            id = poiId,
            name = "Museum",
            description = "Historic museum",
            category = "Culture",
            latitude = 52.0,
            longitude = 21.0,
            metadata = mapOf("opening_hours" to "9-17"),
            imageUrls = listOf(PoiImage(url = "https://example.com/img1.jpg")),
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { poiService.getById(poiId) } returns poi

        // When & Then
        mockMvc.perform(get("/api/pois/{id}", poiId))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(poiId.toString()))
            .andExpect(jsonPath("$.name").value("Museum"))
            .andExpect(jsonPath("$.category").value("Culture"))
            .andExpect(jsonPath("$.latitude").value(52.0))
            .andExpect(jsonPath("$.longitude").value(21.0))

        verify(exactly = 1) { poiService.getById(poiId) }
    }

    @Test
    @WithMockUser
    fun `getPOI should return 404 when POI not found`() {
        // Given
        val poiId = UUID.randomUUID()
        every { poiService.getById(poiId) } throws ResponseStatusException(HttpStatus.NOT_FOUND, "POI not found")

        // When & Then
        mockMvc.perform(get("/api/pois/{id}", poiId))
            .andExpect(status().isNotFound)

        verify(exactly = 1) { poiService.getById(poiId) }
    }

    @Test
    @WithMockUser
    fun `createPOI should create POI and return 201 with Location header`() {
        // Given
        val request = CreatePoiPublicRequest(
            name = "New Museum",
            description = "Brand new museum",
            category = "Culture",
            latitude = 52.0,
            longitude = 21.0,
            metadata = mapOf("opening_hours" to "9-17"),
            imageUrls = listOf(PoiImage(url = "https://example.com/img.jpg"))
        )

        val createdPoi = PoiResponse(
            id = UUID.randomUUID(),
            name = request.name,
            description = request.description,
            category = request.category,
            latitude = request.latitude,
            longitude = request.longitude,
            metadata = request.metadata,
            imageUrls = request.imageUrls,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { poiService.create(any()) } returns createdPoi

        // When & Then
        mockMvc.perform(
            post("/api/pois")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/pois/")))
            .andExpect(jsonPath("$.name").value("New Museum"))
            .andExpect(jsonPath("$.category").value("Culture"))
            .andExpect(jsonPath("$.latitude").value(52.0))

        verify(exactly = 1) { poiService.create(any()) }
    }

    @Test
    @WithMockUser
    fun `createPOI should return 400 for invalid request - blank name`() {
        // Given
        val invalidRequest = CreatePoiPublicRequest(
            name = "",
            description = "Description",
            category = "Culture",
            latitude = 52.0,
            longitude = 21.0,
            metadata = null,
            imageUrls = null
        )

        // When & Then
        mockMvc.perform(
            post("/api/pois")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { poiService.create(any()) }
    }

    @Test
    @WithMockUser
    fun `createPOI should return 400 for invalid request - blank category`() {
        // Given
        val invalidRequest = CreatePoiPublicRequest(
            name = "Museum",
            description = "Description",
            category = "",
            latitude = 52.0,
            longitude = 21.0,
            metadata = null,
            imageUrls = null
        )

        // When & Then
        mockMvc.perform(
            post("/api/pois")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { poiService.create(any()) }
    }

    @Test
    @WithMockUser
    fun `createPOI should create POI with minimal required fields`() {
        // Given
        val request = CreatePoiPublicRequest(
            name = "Minimal POI",
            description = null,
            category = "Category",
            latitude = null,
            longitude = null,
            metadata = null,
            imageUrls = null
        )

        val createdPoi = PoiResponse(
            id = UUID.randomUUID(),
            name = request.name,
            description = null,
            category = request.category,
            latitude = null,
            longitude = null,
            metadata = null,
            imageUrls = null,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { poiService.create(any()) } returns createdPoi

        // When & Then
        mockMvc.perform(
            post("/api/pois")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Minimal POI"))
            .andExpect(jsonPath("$.category").value("Category"))

        verify(exactly = 1) { poiService.create(any()) }
    }

    @Test
    @WithMockUser
    fun `createPOI should return 400 when name exceeds max length`() {
        // Given
        val longName = "A".repeat(201) // Max is 200
        val invalidRequest = CreatePoiPublicRequest(
            name = longName,
            description = "Description",
            category = "Culture",
            latitude = 52.0,
            longitude = 21.0,
            metadata = null,
            imageUrls = null
        )

        // When & Then
        mockMvc.perform(
            post("/api/pois")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { poiService.create(any()) }
    }

    @Test
    fun `createPOI should return 403 when unauthorized`() {
        // Given
        val request = CreatePoiPublicRequest(
            name = "POI",
            description = "Description",
            category = "Category",
            latitude = 52.0,
            longitude = 21.0,
            metadata = null,
            imageUrls = null
        )

        // When & Then
        mockMvc.perform(
            post("/api/pois")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)

        verify(exactly = 0) { poiService.create(any()) }
    }
}
