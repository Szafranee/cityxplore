package app.cityxplore.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.cityxplore.database.entity.FogOfWarEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for fog of war (revealed hexagons) operations.
 *
 * Manages the local cache of H3 hexagons that have been revealed by the user.
 */
@Dao
interface FogOfWarDao {

    /**
     * Observes all revealed hexagon IDs.
     * Emits whenever the revealed hexagons change.
     */
    @Query("SELECT hexagonId FROM fog_of_war")
    fun observeRevealedHexagons(): Flow<List<String>>

    /**
     * Gets all revealed hexagon IDs synchronously.
     */
    @Query("SELECT hexagonId FROM fog_of_war")
    suspend fun getRevealedHexagons(): List<String>

    /**
     * Gets all hexagons that haven't been synced to the server yet.
     */
    @Query("SELECT hexagonId FROM fog_of_war WHERE syncedToServer = 0")
    suspend fun getUnsyncedHexagons(): List<String>

    /**
     * Checks if a hexagon is already revealed.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM fog_of_war WHERE hexagonId = :hexagonId)")
    suspend fun isHexagonRevealed(hexagonId: String): Boolean

    /**
     * Inserts new revealed hexagons, ignoring duplicates.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHexagons(hexagons: List<FogOfWarEntity>)

    /**
     * Marks hexagons as synced to the server.
     */
    @Query("UPDATE fog_of_war SET syncedToServer = 1 WHERE hexagonId IN (:hexagonIds)")
    suspend fun markAsSynced(hexagonIds: List<String>)

    /**
     * Inserts hexagons that came from server sync (already synced).
     */
    suspend fun insertFromServer(hexagonIds: Set<String>) {
        val entities = hexagonIds.map { FogOfWarEntity.create(hexagonId = it, syncedToServer = true) }
        insertHexagons(entities)
    }

    /**
     * Inserts locally revealed hexagons (not yet synced).
     */
    suspend fun insertLocallyRevealed(hexagonIds: Set<String>) {
        val entities = hexagonIds.map { FogOfWarEntity.create(hexagonId = it, syncedToServer = false) }
        insertHexagons(entities)
    }

    /**
     * Gets the count of revealed hexagons.
     */
    @Query("SELECT COUNT(*) FROM fog_of_war")
    suspend fun getRevealedCount(): Int

    /**
     * Clears all fog of war data (used on logout).
     */
    @Query("DELETE FROM fog_of_war")
    suspend fun clearAll()
}
