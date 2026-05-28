package kz.kripto.studycompose1.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.kripto.studycompose1.R
import kz.kripto.studycompose1.components.CreateTeamNavbar
import kz.kripto.studycompose1.components.KineticInput
import kz.kripto.studycompose1.components.KineticPrimaryButton
import kz.kripto.studycompose1.ui.theme.KineticStyle
import kz.kripto.studycompose1.viewModel.TeamViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeamScreen(
    onBack: () -> Unit,
    viewModel: TeamViewModel = koinViewModel()
) {
    val teamName = viewModel.teamNameInput.value
    val isPrivate = viewModel.isPrivateInput.value
    val inviteCode = viewModel.inviteCodeInput.value
    val error = viewModel.teamError.value

    Scaffold(
        topBar = { CreateTeamNavbar(onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            KineticInput(
                value = teamName,
                onValueChange = { viewModel.teamNameInput.value = it },
                label = "Название команды"
            )

            KineticInput(
                value = inviteCode,
                onValueChange = { viewModel.inviteCodeInput.value = it },
                label = "Инвайт-код (необязательно)"
            )

            // Переключатель приватности (Публичный/Приватный как на макете TeamDetail)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPrivate) "Приватная команда" else "Публичная команда",
                    style = KineticStyle.rubikNormalStyle,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Switch(
                    checked = isPrivate,
                    onCheckedChange = { viewModel.isPrivateInput.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            if (error != null) {
                Text(text = error, color = MaterialTheme.colorScheme.error, style = KineticStyle.rubikNormalStyle)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Кнопка: Создать и сохранить изменения
            KineticPrimaryButton(
                text = "Сохранить изменения",
                onClick = { viewModel.createTeam(onSuccess = onBack) }
            )
        }
    }
}