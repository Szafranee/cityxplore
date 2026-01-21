package app.cityxplore.core.notifications
import app.cityxplore.auth.domain.AuthRepository
import app.cityxplore.core.CityXploreDispatchers
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
sealed interface SocialDataChangeEvent {
    data object FriendsChanged : SocialDataChangeEvent
    data object SharedPoisChanged : SocialDataChangeEvent
    data object RankingsChanged : SocialDataChangeEvent
}
class SocialNotificationManager(
    private val supabaseClient: SupabaseClient,
    private val notificationService: NotificationService,
    private val authRepository: AuthRepository,
    dispatchers: CityXploreDispatchers
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private var realtimeJob: Job? = null
    private var currentUserId: String? = null
    private val _dataChangeEvents = MutableSharedFlow<SocialDataChangeEvent>(extraBufferCapacity = 10)
    val dataChangeEvents: SharedFlow<SocialDataChangeEvent> = _dataChangeEvents.asSharedFlow()
    fun startObserving() {
        if (realtimeJob?.isActive == true) return
        realtimeJob = scope.launch {
            currentUserId = authRepository.getCurrentUserId()
            if (currentUserId == null) return@launch
            delay(1000)
            try {
                supabaseClient.realtime.connect()
                setupRealtimeSubscriptions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun stopObserving() {
        realtimeJob?.cancel()
        realtimeJob = null
        scope.launch {
            try { supabaseClient.realtime.removeAllChannels() } catch (_: Exception) {}
        }
        currentUserId = null
    }
    private suspend fun setupRealtimeSubscriptions() {
        val userId = currentUserId ?: return
        val friendshipsChannel = supabaseClient.realtime.channel("social-friendships-$userId")
        friendshipsChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "friendships"
        }.onEach { change -> handleFriendshipInsert(change, userId) }.catch { }.launchIn(scope)
        friendshipsChannel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "friendships"
        }.onEach { change -> handleFriendshipUpdate(change, userId) }.catch { }.launchIn(scope)
        friendshipsChannel.subscribe()
        val sharedPoisChannel = supabaseClient.realtime.channel("social-shared-pois-$userId")
        sharedPoisChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "shared_pois"
        }.onEach { change -> handleSharedPoiInsert(change, userId) }.catch { }.launchIn(scope)
        sharedPoisChannel.subscribe()
    }
    private suspend fun handleFriendshipInsert(change: PostgresAction.Insert, userId: String) {
        val record = change.record
        val requesterId = record["requester_id"]?.jsonPrimitive?.content
        val addresseeId = record["addressee_id"]?.jsonPrimitive?.content
        val status = record["status"]?.jsonPrimitive?.content
        _dataChangeEvents.tryEmit(SocialDataChangeEvent.FriendsChanged)
        if (addresseeId == userId && status == "pending" && requesterId != null) {
            val requesterName = fetchUsername(requesterId) ?: "Someone"
            notificationService.showFriendRequestNotification(fromUsername = requesterName)
        }
    }
    private suspend fun handleFriendshipUpdate(change: PostgresAction.Update, userId: String) {
        val record = change.record
        val requesterId = record["requester_id"]?.jsonPrimitive?.content
        val addresseeId = record["addressee_id"]?.jsonPrimitive?.content
        val status = record["status"]?.jsonPrimitive?.content?.lowercase()
        _dataChangeEvents.tryEmit(SocialDataChangeEvent.FriendsChanged)
        when {
            status == "pending" && addresseeId == userId && requesterId != null -> {
                val requesterName = fetchUsername(requesterId) ?: "Someone"
                notificationService.showFriendRequestNotification(fromUsername = requesterName)
            }
            status == "accepted" && requesterId == userId && addresseeId != null -> {
                _dataChangeEvents.tryEmit(SocialDataChangeEvent.RankingsChanged)
                val accepterName = fetchUsername(addresseeId) ?: "A user"
                notificationService.showFriendRequestAcceptedNotification(username = accepterName)
            }
            status == "accepted" && addresseeId == userId -> {
                _dataChangeEvents.tryEmit(SocialDataChangeEvent.RankingsChanged)
            }
        }
    }
    private suspend fun handleSharedPoiInsert(change: PostgresAction.Insert, userId: String) {
        val record = change.record
        val sharerId = record["sharer_id"]?.jsonPrimitive?.content
        val recipientId = record["recipient_id"]?.jsonPrimitive?.content
        val message = record["message"]?.jsonPrimitive?.content
        _dataChangeEvents.tryEmit(SocialDataChangeEvent.SharedPoisChanged)
        if (recipientId == userId && sharerId != null) {
            val sharerName = fetchUsername(sharerId) ?: "A friend"
            val poiData = record["poi_data"]
            val poiName = try { poiData?.jsonObject?.get("name")?.jsonPrimitive?.content ?: "A location" } catch (_: Exception) { "A location" }
            notificationService.showSharedPoiNotification(sharerName = sharerName, poiName = poiName, message = message)
        }
    }
    private suspend fun fetchUsername(usrId: String): String? {
        return try {
            supabaseClient.postgrest.from("users").select { filter { eq("id", usrId) } }.decodeSingleOrNull<UserNameDto>()?.username
        } catch (_: Exception) { null }
    }
}
@kotlinx.serialization.Serializable
private data class UserNameDto(val username: String? = null)
