package kz.kripto.studycompose1.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kz.kripto.studycompose1.database.dao.TeamDao
import kz.kripto.studycompose1.database.entities.TeamEntity
import kz.kripto.studycompose1.database.entities.TeamMemberEntity
import java.util.Collections

/**
 * Репозиторий для управления командами.
 * Здесь происходит вся магия: создание команд, вступление по коду и синхронизация участников.
 */
class TeamRepository(
    private val teamDao: TeamDao,
    private val userRepository: UserRepository,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeListeners = mutableListOf<ListenerRegistration>()
    private val memberListenersMap = mutableMapOf<String, ListenerRegistration>()
    private val updatingInviteCodes = Collections.synchronizedSet(mutableSetOf<String>())

    init {
        // Как только вошли в аккаунт — начинаем слушать команды из облака
        auth.addAuthStateListener { startRealtimeSync() }
    }

    /**
     * Слежу за списком команд в Firebase.
     * Если команда удалена в облаке — она должна исчезнуть и на телефоне.
     */
    fun startRealtimeSync() {
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
        memberListenersMap.values.forEach { it.remove() }
        memberListenersMap.clear()
        
        val teamListener = firestore.collection("teams")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                
                snapshots?.documentChanges?.forEach { change ->
                    val inviteCode = change.document.id
                    
                    repositoryScope.launch {
                        if (change.type == DocumentChange.Type.REMOVED) {
                            // Команду удалили из Firestore — стираем её локально
                            val team = teamDao.getTeamByCode(inviteCode)
                            if (team != null) {
                                teamDao.deleteTeam(team)
                                memberListenersMap[inviteCode]?.remove()
                                memberListenersMap.remove(inviteCode)
                            }
                        } else {
                            // Команда добавлена или изменилась — обновляем данные в Room
                            val teamData = change.document.data
                            syncTeamToLocal(inviteCode, teamData)
                            // Начинаем следить за участниками этой команды
                            startMembersSync(inviteCode)
                        }
                    }
                }
            }
        activeListeners.add(teamListener)
    }

    /**
     * Слушаю список участников конкретной команды.
     * Если кто-то вступил или сменил роль — это сразу отобразится у всех.
     */
    private fun startMembersSync(inviteCode: String) {
        if (memberListenersMap.containsKey(inviteCode)) return
        
        val listener = firestore.collection("teams").document(inviteCode)
            .collection("members")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                
                snapshots?.documentChanges?.forEach { change ->
                    val memberUid = change.document.id
                    val role = change.document.getString("role") ?: "member"
                    
                    repositoryScope.launch {
                        val teamId = teamDao.getTeamByCode(inviteCode)?.id ?: return@launch
                        if (change.type != DocumentChange.Type.REMOVED) {
                            // Добавляем или обновляем участника
                            val localUserId = userRepository.fetchAndSaveUserProfile(memberUid)
                            if (localUserId != -1L) {
                                teamDao.insertMember(TeamMemberEntity(userId = localUserId, teamId = teamId, role = role))
                            }
                        } else {
                            // Кто-то покинул команду — убираем его локально
                            val localUser = userRepository.fetchAndSaveUserProfile(memberUid)
                            if (localUser != -1L) teamDao.removeMember(teamId, localUser)
                        }
                    }
                }
            }
        memberListenersMap[inviteCode] = listener
        activeListeners.add(listener)
    }

    /**
     * Сохраняю данные команды из облака в локальный Room.
     * Важно: вычисляем локального создателя через UID, чтобы на разных телефонах всё работало четко.
     */
    private suspend fun syncTeamToLocal(inviteCode: String, data: Map<String, Any>) {
        val existingTeam = teamDao.getTeamByCode(inviteCode)
        val creatorUid = data["creatorUid"] as? String
        
        val localCreatorId = if (creatorUid != null) {
            userRepository.fetchAndSaveUserProfile(creatorUid)
        } else {
            (data["creatorId"] as? Number)?.toLong() ?: 0L
        }

        val team = TeamEntity(
            id = existingTeam?.id ?: 0L,
            teamName = data["teamName"] as? String ?: "",
            inviteCode = inviteCode,
            creatorId = localCreatorId,
            creatorUid = creatorUid,
            isPrivate = data["isPrivate"] as? Boolean ?: false
        )
        teamDao.insertTeam(team)
    }

    /**
     * Создаю новую команду. Сначала пишу в телефон, потом отправляю в Firebase.
     * Создатель сразу получает роль "admin".
     */
    suspend fun createTeam(teamName: String, inviteCode: String?, creatorId: Long, isPrivate: Boolean) {
        val finalCode = inviteCode ?: "KNT-${(100000..999999).random()}"
        val uid = auth.currentUser?.uid
        updatingInviteCodes.add(finalCode)

        val localId = teamDao.insertTeam(
            TeamEntity(
                teamName = teamName,
                inviteCode = finalCode,
                creatorId = creatorId,
                creatorUid = uid,
                isPrivate = isPrivate
            )
        )
        // Локально сразу ставим роль админа
        teamDao.insertMember(TeamMemberEntity(userId = creatorId, teamId = localId, role = "admin"))

        val teamMap = hashMapOf("teamName" to teamName, "inviteCode" to finalCode, "creatorId" to creatorId, "creatorUid" to uid, "isPrivate" to isPrivate)
        try {
            // Пишем основную информацию о команде
            firestore.collection("teams").document(finalCode).set(teamMap).await()
            if (uid != null) {
                // Добавляем создателя в подколлекцию участников в облаке
                firestore.collection("teams").document(finalCode).collection("members").document(uid).set(mapOf("role" to "admin")).await()
            }
        } catch (e: Exception) {
            updatingInviteCodes.remove(finalCode)
        }
    }

    /**
     * Вступаю в команду по коду приглашения.
     * Качаю данные команды и добавляю себя в её список участников в Firestore.
     */
    suspend fun joinTeamByCode(inviteCode: String, userId: Long): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val doc = firestore.collection("teams").document(inviteCode).get().await()
            if (doc.exists()) {
                syncTeamToLocal(inviteCode, doc.data!!)
                val teamId = teamDao.getTeamByCode(inviteCode)?.id ?: return false
                teamDao.insertMember(TeamMemberEntity(userId = userId, teamId = teamId))
                
                // В облаке по умолчанию роль "member"
                firestore.collection("teams").document(inviteCode)
                    .collection("members").document(uid)
                    .set(mapOf("role" to "member", "joinedAt" to System.currentTimeMillis()))
                    .await()
                true
            } else false
        } catch (e: Exception) { false }
    }

    /**
     * Вступление в открытую (публичную) команду.
     */
    suspend fun joinPublicTeam(teamId: Long, userId: Long) {
        val uid = auth.currentUser?.uid ?: return
        val team = teamDao.getTeamByIdOnce(teamId) ?: return
        val inviteCode = team.inviteCode

        teamDao.insertMember(TeamMemberEntity(userId = userId, teamId = teamId))

        try {
            firestore.collection("teams").document(inviteCode)
                .collection("members").document(uid)
                .set(mapOf("role" to "member", "joinedAt" to System.currentTimeMillis()))
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Удаление команды полностью. Стираем везде.
     */
    suspend fun deleteTeam(team: TeamEntity) {
        val inviteCode = team.inviteCode
        updatingInviteCodes.add(inviteCode)
        teamDao.deleteTeam(team)
        try {
            firestore.collection("teams").document(inviteCode).delete().await()
        } catch (e: Exception) {
            updatingInviteCodes.remove(inviteCode)
        }
    }

    /**
     * Обновление роли участника (например, назначение мл. админом). 
     * Только для владельца команды.
     */
    suspend fun updateMemberRole(inviteCode: String, memberUid: String, newRole: String) {
        try {
            firestore.collection("teams").document(inviteCode)
                .collection("members").document(memberUid)
                .update("role", newRole)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
