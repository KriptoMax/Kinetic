package kz.kripto.studycompose1.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.kripto.studycompose1.R
import kz.kripto.studycompose1.components.AuthNavbar
import kz.kripto.studycompose1.components.KineticInput
import kz.kripto.studycompose1.components.KineticPrimaryButton
import kz.kripto.studycompose1.components.KineticTextButton
import kz.kripto.studycompose1.ui.theme.KineticStyle
import kz.kripto.studycompose1.viewModel.AuthViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onBackClick: () -> Unit, // 1. ДОБАВИЛИ КЛЮЧЕВОЙ КОЛБЕК ДЛЯ НАВИГАЦИИ НАЗАД
    viewModel: AuthViewModel = koinViewModel()
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    val error by viewModel.authError

    LaunchedEffect(key1 = viewModel.authSuccess) {
        viewModel.authSuccess.collect { success: Boolean ->
            if (success) {
                onAuthSuccess()
            }
        }
    }

    Scaffold(
        topBar = {
            AuthNavbar(
                isRegisterMode = isRegisterMode,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            KineticInput(
                value = viewModel.email.value,
                onValueChange = { viewModel.email.value = it },
                label = "Email"
            )

            if (isRegisterMode) {
                Spacer(modifier = Modifier.height(16.dp))
                KineticInput(
                    value = viewModel.username.value,
                    onValueChange = { viewModel.username.value = it },
                    label = "Логин"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            KineticInput(
                value = viewModel.password.value,
                onValueChange = { viewModel.password.value = it },
                label = "Пароль",
                visualTransformation = PasswordVisualTransformation()
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = KineticStyle.rubikNormalStyle,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Главная зеленая КНОПКА
            KineticPrimaryButton(
                text = if (isRegisterMode) "Зарегистрироваться" else "Войти",
                onClick = {
                    if (isRegisterMode) viewModel.register() else viewModel.login()
                },
                borderColor = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Переключалка режима Вход/Регистрация
            KineticTextButton(
                text = if (isRegisterMode) "Уже есть аккаунт? Войти" else "Нет аккаунта? Создать",
                onClick = { isRegisterMode = !isRegisterMode }
            )
        }
    }
}