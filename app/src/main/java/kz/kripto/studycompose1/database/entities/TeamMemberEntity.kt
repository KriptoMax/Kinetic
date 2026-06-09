package kz.kripto.studycompose1.database.entities

import androidx.room.Entity

@Entity(
    tableName = "team_members",
    primaryKeys = ["userId", "teamId"]
)
data class TeamMemberEntity(
    val userId: Long,
    val teamId: Long,
    val role: String = "member" // "admin", "junior_admin", "member"
)
