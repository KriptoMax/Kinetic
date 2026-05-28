package kz.kripto.studycompose1.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teamName: String,
    val inviteCode: String,      // Код команды
    val creatorId: Long,         // ИМЕННО ЭТО ПОЛЕ ИЩЕТ ВЬЮМОДЕЛЬ!
    val isPrivate: Boolean = false
)