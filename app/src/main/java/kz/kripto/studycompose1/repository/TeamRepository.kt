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
        auth.addAuthStateListener { startRealtimeSync() }
    }

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
                            // Если команда удалена в облаке, удаляем её локально
                            val team = teamDao.getTeamByCode(inviteCode)
                            if (team != null) {
                                teamDao.deleteTeam(team)
                                // Также убираем слушатель участников для этой команды
                                memberListenersMap[inviteCode]?.remove()
                                memberListenersMap.remove(inviteCode)
                            }
                        } else {
                            // Если добавлена или изменена
                            val teamData = change.document.data
                            syncTeamToLocal(inviteCode, teamData)
                            startMembersSync(inviteCode)
                        }
                    }
                }
            }
        activeListeners.add(teamListener)
    }

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
                            val localUserId = userRepository.fetchAndSaveUserProfile(memberUid)
                            if (localUserId != -1L) {
                                teamDao.insertMember(TeamMemberEntity(userId = localUserId, teamId = teamId, role = role))
                            }
                        } else {
                            val localUser = userRepository.fetchAndSaveUserProfile(memberUid)
                            if (localUser != -1L) teamDao.removeMember(teamId, localUser)
                        }
                    }
                }
            }
        memberListenersMap[inviteCode] = listener
        activeListeners.add(listener)
    }

    private suspend fun syncTeamToLocal(inviteCode: String, data: Map<String, Any>) {
        val existingTeam = teamDao.getTeamByCode(inviteCode)
        val creatorUid = data["creatorUid"] as? String
        
        // КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: Разрешаем локальный ID создателя через его UID,
        // чтобы не брать чужой локальный ID с другого телефона.
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
        // Сразу ставим роль админа локально
        teamDao.insertMember(TeamMemberEntity(userId = creatorId, teamId = localId, role = "admin"))

        val teamMap = hashMapOf("teamName" to teamName, "inviteCode" to finalCode, "creatorId" to creatorId, "creatorUid" to uid, "isPrivate" to isPrivate)
        try {
            firestore.collection("teams").document(finalCode).set(teamMap).await()
            if (uid != null) {
                firestore.collection("teams").document(finalCode).collection("members").document(uid).set(mapOf("role" to "admin")).await()
            }
        } catch (e: Exception) {
            updatingInviteCodes.remove(finalCode)
        }
    }

    suspend fun joinTeamByCode(inviteCode: String, userId: Long): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val doc = firestore.collection("teams").document(inviteCode).get().await()
            if (doc.exists()) {
                syncTeamToLocal(inviteCode, doc.data!!)
                val teamId = teamDao.getTeamByCode(inviteCode)?.id ?: return false
                teamDao.insertMember(TeamMemberEntity(userId = userId, teamId = teamId))
                
                // Добавляем запись в Firestore (Роль: Участник)
                firestore.collection("teams").document(inviteCode)
                    .collection("members").document(uid)
                    .set(mapOf("role" to "member", "joinedAt" to System.currentTimeMillis()))
                    .await()
                true
            } else false
        } catch (e: Exception) { false }
    }

    // Вступление в публичную команду через ID
    suspend fun joinPublicTeam(teamId: Long, userId: Long) {
        val uid = auth.currentUser?.uid ?: return
        val team = teamDao.getTeamByIdOnce(teamId) ?: return
        val inviteCode = team.inviteCode

        // 1. Локально
        teamDao.insertMember(TeamMemberEntity(userId = userId, teamId = teamId))

        // 2. В облако (Роль: Участник)
        try {
            firestore.collection("teams").document(inviteCode)
                .collection("members").document(uid)
                .set(mapOf("role" to "member", "joinedAt" to System.currentTimeMillis()))
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
