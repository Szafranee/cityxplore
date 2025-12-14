package app.cityxplore.map.data

import app.cityxplore.map.domain.PoiCategory
import app.cityxplore.map.domain.PoiModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

interface PoiRepository {
    suspend fun fetchPois(): Result<List<PoiModel>>
    suspend fun discoverPoi(id: String): Result<Unit>
}

class NetworkPoiRepository(
    private val client: HttpClient
) : PoiRepository {
    override suspend fun fetchPois(): Result<List<PoiModel>> = runCatching {
        client.get("https://api.cityxplore.app/api/pois").body<List<PoiDto>>().map(PoiDto::toDomain)
    }

    override suspend fun discoverPoi(id: String): Result<Unit> = runCatching {
        client.post("https://api.cityxplore.app/api/pois/$id/discover") {
            contentType(ContentType.Application.Json)
            setBody(emptyMap<String, String>())
        }
    }.map { }
}

class FakePoiRepository : PoiRepository {
    override suspend fun fetchPois(): Result<List<PoiModel>> = Result.success(
        listOf(
            PoiModel("1", "Old Town", "Historic square", 52.2297, 21.0122, true, PoiCategory.HISTORICAL),
            PoiModel("2", "Vistula Park", "Green escape", 52.25, 21.00, false, PoiCategory.CULTURAL)
        )
    )

    override suspend fun discoverPoi(id: String): Result<Unit> = Result.success(Unit)
}
