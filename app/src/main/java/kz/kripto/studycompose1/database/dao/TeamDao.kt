package kz.kripto.studycompose1.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kz.kripto.studycompose1.database.entities.TeamEntity
import kz.kripto.studycompose1.database.entities.TeamMemberEntity
import kz.kripto.studycompose1.database.entities.UserEntity

data class MemberWithRole(
    val username: String,
    val firebaseUid: String,
    val role: String
)

@Dao
interface TeamDao {

    // Добавить новую команду в таблицу, если ID уже есть — заменить
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity): Long

    // Добавить пользователя в список участников команды
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: TeamMemberEntity)

    // Обновить настройки команды (название, приватность)
    @Update
    suspend fun updateTeam(team: TeamEntity)

    // Полностью удалить команду из базы
    @Delete
    suspend fun deleteTeam(team: TeamEntity)

    // Удалить конкретного участника из команды по его ID
    @Query("DELETE FROM team_members WHERE teamId = :teamId AND userId = :userId")
    suspend fun removeMember(teamId: Long, userId: Long)

    // Удалить участника из команды, найдя его ID по имени пользователя (логину)
    @Query("""
        DELETE FROM team_members 
        WHERE teamId = :teamId AND userId = (SELECT id FROM users WHERE username = :username LIMIT 1)
    """)
    suspend fun removeMemberByUsername(teamId: Long, username: String)
    
    // Создать команду и сразу добавить создателя в её участники (всё в одной транзакции)
    @Transaction
    suspend fun createTeam(teamName: String, inviteCode: String?, creatorId: Long, isPrivate: Boolean) {
        val finalCode = inviteCode ?: "KNT-${(100000..999999).random()}"
        val teamId = insertTeam(
            TeamEntity(teamName = teamName, inviteCode = finalCode, creatorId = creatorId, isPrivate = isPrivate)
        )
        // Сразу добавляем создателя в участники
        insertMember(TeamMemberEntity(userId = creatorId, teamId = teamId))
    }
    
    // Найти команду по коду и добавить туда пользователя
    @Transaction
    suspend fun joinTeamByCode(inviteCode: String, userId: Long): Boolean {
        val team = getTeamByCode(inviteCode) ?: return false
        insertMember(TeamMemberEntity(userId = userId, teamId = team.id))
        return true
    }

    // Проверить, состоит ли человек в этой команде (возвращает true/false в реальном времени)
    @Query("SELECT COUNT(*) > 0 FROM team_members WHERE teamId = :teamId AND userId = :userId")
    fun isUserInTeam(teamId: Long, userId: Long): Flow<Boolean>

    // Просто добавить пользователя в команду по её ID
    @Transaction
    suspend fun joinTeamById(teamId: Long, userId: Long) {
        insertMember(TeamMemberEntity(userId = userId, teamId = teamId))
    }

    // Получить полные профили всех участников конкретной команды
    @Query("""
        SELECT users.* FROM users 
        INNER JOIN team_members ON users.id = team_members.userId 
        WHERE team_members.teamId = :teamId
    """)
    fun getTeamMembersEntities(teamId: Long): Flow<List<UserEntity>>

    // Получить список участников с их ролями (Админ, Мл. Админ и т.д.), сгруппировав по UID
    @Query("""
        SELECT users.username, users.firebaseUid, team_members.role FROM users 
        INNER JOIN team_members ON users.id = team_members.userId 
        WHERE team_members.teamId = :teamId
        GROUP BY users.firebaseUid
    """)
    fun getTeamMembersWithRoles(teamId: Long): Flow<List<MemberWithRole>>

    // Получить только список имен (логинов) всех участников команды
    @Query("""
        SELECT users.username FROM users 
        INNER JOIN team_members ON users.id = team_members.userId 
        WHERE team_members.teamId = :teamId
    """)
    fun getTeamMembers(teamId: Long): Flow<List<String>>

    // Найти команду по её уникальному пригласительному коду
    @Query("SELECT * FROM teams WHERE inviteCode = :inviteCode LIMIT 1")
    suspend fun getTeamByCode(inviteCode: String): TeamEntity?

    // Следить за данными команды по её ID
    @Query("SELECT * FROM teams WHERE id = :teamId LIMIT 1")
    fun getTeamById(teamId: Long): Flow<TeamEntity?>

    // Получить данные команды по ID один раз (без подписки)
    @Query("SELECT * FROM teams WHERE id = :teamId LIMIT 1")
    suspend fun getTeamByIdOnce(teamId: Long): TeamEntity?

    // Изменить роль участника в команде (находит пользователя по глобальному UID)
    @Query("UPDATE team_members SET role = :newRole WHERE teamId = :teamId AND userId = (SELECT id FROM users WHERE firebaseUid = :firebaseUid LIMIT 1)")
    suspend fun updateMemberRole(teamId: Long, firebaseUid: String, newRole: String)

    // Получить все команды для пользователя: те, где он состоит, ПЛЮС все публичные команды
    @Query("""
        SELECT DISTINCT teams.* FROM teams 
        LEFT JOIN team_members ON teams.id = team_members.teamId 
        WHERE team_members.userId = :userId OR teams.isPrivate = 0
    """)
    fun getTeamsForUser(userId: Long): Flow<List<TeamEntity>>
}