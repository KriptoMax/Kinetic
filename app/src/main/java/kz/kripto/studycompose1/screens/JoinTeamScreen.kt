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
import kz.kripto.studycompose1.components.JoinTeamNavbar
import kz.kripto.studycompose1.components.KineticInput
import kz.kripto.studycompose1.components.KineticPrimaryButton
import kz.kripto.studycompose1.ui.theme.KineticStyle
import kz.kripto.studycompose1.viewModel.TeamViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinTeamScreen(
    onBack: () -> Unit,
    viewModel: TeamViewModel = koinViewModel()
) {
    val inviteCode = viewModel.inviteCodeInput.value
    val error = viewModel.teamError.value

    Scaffold(
        topBar = { JoinTeamNavbar(onBack = onBack) }
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
            Text(
                text = "Введите уникальный код приглашения, чтобы присоединиться к существующей группе.",
                style = KineticStyle.rubikNormalStyle,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            KineticInput(
                value = inviteCode,
                onValueChange = { viewModel.inviteCodeInput.value = it },
                label = "Код команды (например, KNT-123456)"
            )

            if (error != null) {
                Text(text = error, color = MaterialTheme.colorScheme.error, style = KineticStyle.rubikNormalStyle)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Твой макет: Кнопка "Подтвердить"
            KineticPrimaryButton(
                text = "Подтвердить",
                onClick = { viewModel.joinTeam(onSuccess = onBack) }
            )
        }
    }
}