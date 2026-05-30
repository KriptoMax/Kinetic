package kz.kripto.studycompose1.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teamName: String,
    val inviteCode: String,      // Код команды
    val creatorId: Long,         // Локальный ID
    val creatorUid: String? = null, // Глобальный ID (Firebase UID)
    val isPrivate: Boolean = false
)
