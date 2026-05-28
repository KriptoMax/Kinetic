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

@Dao
interface TeamDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: TeamMemberEntity)

    @Update
    suspend fun updateTeam(team: TeamEntity)

    @Delete
    suspend fun deleteTeam(team: TeamEntity)

    @Query("DELETE FROM team_members WHERE teamId = :teamId AND userId = :userId")
    suspend fun removeMember(teamId: Long, userId: Long)

    @Query("""
        DELETE FROM team_members 
        WHERE teamId = :teamId AND userId = (SELECT id FROM users WHERE username = :username LIMIT 1)
    """)
    suspend fun removeMemberByUsername(teamId: Long, username: String)

    // ИСПРАВЛЕНИЕ ОШИБКИ 'createTeam'
    @Transaction
    suspend fun createTeam(teamName: String, inviteCode: String?, creatorId: Long, isPrivate: Boolean) {
        val finalCode = inviteCode ?: "KNT-${(100000..999999).random()}"
        val teamId = insertTeam(
            TeamEntity(teamName = teamName, inviteCode = finalCode, creatorId = creatorId, isPrivate = isPrivate)
        )
        // Сразу добавляем создателя в участники
        insertMember(TeamMemberEntity(userId = creatorId, teamId = teamId))
    }

    // ИСПРАВЛЕНИЕ ОШИБКИ 'joinTeamByCode'
    @Transaction
    suspend fun joinTeamByCode(inviteCode: String, userId: Long): Boolean {
        val team = getTeamByCode(inviteCode) ?: return false
        insertMember(TeamMemberEntity(userId = userId, teamId = team.id))
        return true
    }

    @Query("SELECT COUNT(*) > 0 FROM team_members WHERE teamId = :teamId AND userId = :userId")
    fun isUserInTeam(teamId: Long, userId: Long): Flow<Boolean>

    @Transaction
    suspend fun joinTeamById(teamId: Long, userId: Long) {
        insertMember(TeamMemberEntity(userId = userId, teamId = teamId))
    }

    @Query("""
        SELECT users.* FROM users 
        INNER JOIN team_members ON users.id = team_members.userId 
        WHERE team_members.teamId = :teamId
    """)
    fun getTeamMembersEntities(teamId: Long): Flow<List<UserEntity>>

    @Query("""
        SELECT users.username FROM users 
        INNER JOIN team_members ON users.id = team_members.userId 
        WHERE team_members.teamId = :teamId
    """)
    fun getTeamMembers(teamId: Long): Flow<List<String>>

    @Query("SELECT * FROM teams WHERE inviteCode = :inviteCode LIMIT 1")
    suspend fun getTeamByCode(inviteCode: String): TeamEntity?

    @Query("SELECT * FROM teams WHERE id = :teamId LIMIT 1")
    fun getTeamById(teamId: Long): Flow<TeamEntity?>

    @Query("""
        SELECT DISTINCT teams.* FROM teams 
        LEFT JOIN team_members ON teams.id = team_members.teamId 
        WHERE team_members.userId = :userId OR teams.isPrivate = 0
    """)
    fun getTeamsForUser(userId: Long): Flow<List<TeamEntity>>
}