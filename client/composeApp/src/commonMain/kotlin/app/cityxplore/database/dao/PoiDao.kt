package app.cityxplore.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.cityxplore.database.entity.PoiEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Point of Interest operations.
 *
 * Provides methods to read and write POI data to the local database,
 * including discovery and favorite status management.
 */
@Dao
interface PoiDao {

    /**
     * Observes all POIs with their current status.
     * Emits whenever any POI data changes.
     */
    @Query("SELECT * FROM pois ORDER BY name ASC")
    fun observeAllPois(): Flow<List<PoiEntity>>

    /**
     * Observes only discovered POIs.
     */
    @Query("SELECT * FROM pois WHERE discovered = 1 ORDER BY discoveryDate DESC")
    fun observeDiscoveredPois(): Flow<List<PoiEntity>>

    /**
     * Observes only favorite POIs.
     */
    @Query("SELECT * FROM pois WHERE isFavorite = 1 ORDER BY name ASC")
    fun observeFavoritePois(): Flow<List<PoiEntity>>

    /**
     * Gets all POIs synchronously.
     */
    @Query("SELECT * FROM pois")
    suspend fun getAllPois(): List<PoiEntity>

    /**
     * Gets a single POI by ID.
     */
    @Query("SELECT * FROM pois WHERE id = :poiId")
    suspend fun getPoiById(poiId: String): PoiEntity?

    /**
     * Inserts or updates multiple POIs.
     */
    @Upsert
    suspend fun upsertAll(pois: List<PoiEntity>)

    /**
     * Inserts or updates a single POI.
     */
    @Upsert
    suspend fun upsertPoi(poi: PoiEntity)

    /**
     * Marks a POI as discovered with the current timestamp.
     */
    @Query(
        """
        UPDATE pois 
        SET discovered = 1, 
            discoveryDate = :discoveryDate, 
            lastSyncedAt = :syncedAt 
        WHERE id = :poiId
    """
    )
    suspend fun markAsDiscovered(
        poiId: String,
        discoveryDate: Long,
        syncedAt: Long
    )

    /**
     * Toggles the favorite status of a POI.
     */
    @Query("UPDATE pois SET isFavorite = NOT isFavorite, lastSyncedAt = :syncedAt WHERE id = :poiId")
    suspend fun toggleFavorite(poiId: String, syncedAt: Long)

    /**
     * Sets the favorite status of a POI.
     */
    @Query("UPDATE pois SET isFavorite = :isFavorite, lastSyncedAt = :syncedAt WHERE id = :poiId")
    suspend fun setFavorite(poiId: String, isFavorite: Boolean, syncedAt: Long)

    /**
     * Gets the count of discovered POIs.
     */
    @Query("SELECT COUNT(*) FROM pois WHERE discovered = 1")
    suspend fun getDiscoveredCount(): Int

    /**
     * Gets the timestamp of the last sync.
     */
    @Query("SELECT MAX(lastSyncedAt) FROM pois")
    suspend fun getLastSyncTimestamp(): Long?

    /**
     * Clears all POI data (used on logout).
     */
    @Query("DELETE FROM pois")
    suspend fun clearAll()
}
