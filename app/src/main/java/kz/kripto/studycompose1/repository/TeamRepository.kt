package kz.kripto.studycompose1.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kz.kripto.studycompose1.database.dao.TeamDao
import kz.kripto.studycompose1.database.entities.TeamEntity
import kz.kripto.studycompose1.database.entities.TeamMemberEntity
import com.google.firebase.firestore.DocumentChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.google.firebase.firestore.ListenerRegistration
import java.util.Collections

// Мой репозиторий для управления командами
class TeamRepository(
    private val teamDao: TeamDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var teamListener: ListenerRegistration? = null
    
    // Список кодов команд, которые я сейчас обновляю, чтобы не зациклиться при синхронизации
    private val updatingInviteCodes = Collections.synchronizedSet(mutableSetOf<String>())

    init {
        // Подписываюсь на изменения при старте (если залогинен)
        auth.addAuthStateListener {
            startRealtimeSync()
        }
    }

    // Слушаю изменения команд в облаке и обновляю свой Room
    fun startRealtimeSync() {
        teamListener?.remove()
        
        teamListener = firestore.collection("teams")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    e.printStackTrace()
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { change ->
                    val doc = change.document
                    val data = doc.data
                    val remoteId = doc.id

                    // Игнорирую эхо от моих же правок
                    if (updatingInviteCodes.contains(remoteId)) {
                        if (!doc.metadata.hasPendingWrites()) {
                            updatingInviteCodes.remove(remoteId)
                        }
                        return@forEach
                    }

                    repositoryScope.launch {
                        when (change.type) {
                            // Синхронизирую новую или измененную команду
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                syncTeamToLocal(remoteId, data)
                            }
                            // Если команду удалили из облака, удаляю её и у себя в телефоне
                            DocumentChange.Type.REMOVED -> {
                                teamDao.getTeamByCode(remoteId)?.let { teamDao.deleteTeam(it) }
                            }
                        }
                    }
                }
            }
    }

    // Хелпер для записи данных о команде из облака в локальный Room
    private suspend fun syncTeamToLocal(inviteCode: String, data: Map<String, Any>) {
        val existingTeam = teamDao.getTeamByCode(inviteCode)
        val team = TeamEntity(
            id = existingTeam?.id ?: 0L,
            teamName = data["teamName"] as? String ?: "",
            inviteCode = inviteCode,
            creatorId = (data["creatorId"] as? Number)?.toLong() ?: 0L,
            creatorUid = data["creatorUid"] as? String, // Сохраняю глобальный ID основателя
            isPrivate = data["isPrivate"] as? Boolean ?: false
        )
        teamDao.insertTeam(team)
    }

    // Создаю новую команду отовсюду: и у себя в телефоне, и в облаке
    suspend fun createTeam(teamName: String, inviteCode: String?, creatorId: Long, isPrivate: Boolean) {
        val finalCode = inviteCode ?: "KNT-${(100000..999999).random()}"
        val uid = auth.currentUser?.uid
        val username = teamDao.getTeamMembers(0).first().firstOrNull() ?: "Owner" // Просто берем имя текущего

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
        teamDao.insertMember(TeamMemberEntity(userId = creatorId, teamId = localId))

        val teamMap = hashMapOf(
            "teamName" to teamName,
            "inviteCode" to finalCode,
            "creatorId" to creatorId,
            "creatorUid" to uid,
            "isPrivate" to isPrivate
        )
        
        try {
            // Создаем команду
            firestore.collection("teams").document(finalCode).set(teamMap).await()
            // Добавляем создателя в список участников в облаке
            if (uid != null) {
                val memberMap = hashMapOf("uid" to uid, "role" to "admin")
                firestore.collection("teams").document(finalCode)
                    .collection("members").document(uid).set(memberMap).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            updatingInviteCodes.remove(finalCode)
        }
    }

    // Метод для вступления в команду по её секретному коду
    suspend fun joinTeamByCode(inviteCode: String, userId: Long): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val doc = firestore.collection("teams").document(inviteCode).get().await()
            if (doc.exists()) {
                val data = doc.data ?: return false
                syncTeamToLocal(inviteCode, data)
                
                val teamId = teamDao.getTeamByCode(inviteCode)?.id ?: return false
                teamDao.insertMember(TeamMemberEntity(userId = userId, teamId = teamId))

                // Добавляем пользователя в список участников в облаке Firestore
                val memberMap = hashMapOf("uid" to uid, "role" to "member")
                firestore.collection("teams").document(inviteCode)
                    .collection("members").document(uid).set(memberMap).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // Удаляю команду отовсюду
    suspend fun deleteTeam(team: TeamEntity) {
        val inviteCode = team.inviteCode
        updatingInviteCodes.add(inviteCode)

        teamDao.deleteTeam(team)
        try {
            firestore.collection("teams").document(inviteCode).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
            updatingInviteCodes.remove(inviteCode)
        }
    }
}
