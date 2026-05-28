package kz.kripto.studycompose1.database.entities

import androidx.room.Entity

@Entity(
    tableName = "team_members",
    primaryKeys = ["userId", "teamId"] // Составной ключ, чтобы один юзер не вступал в одну команду дважды
)
data class TeamMemberEntity(
    val userId: Long,
    val teamId: Long
)