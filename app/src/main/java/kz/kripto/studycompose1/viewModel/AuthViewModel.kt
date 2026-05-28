package kz.kripto.studycompose1.viewModel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kz.kripto.studycompose1.database.data.SessionManager
import kz.kripto.studycompose1.database.dao.UserDao
import kz.kripto.studycompose1.database.entities.UserEntity

class AuthViewModel(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val auth: FirebaseAuth
) : ViewModel() {

    var username = mutableStateOf("")
    var password = mutableStateOf("")

    var authError = mutableStateOf<String?>(null)

    private val _authSuccess = MutableSharedFlow<Boolean>()
    val authSuccess = _authSuccess.asSharedFlow()

    fun register() {
        if (username.value.isBlank() || password.value.isBlank()) {
            authError.value = "Заполните все поля"
            return
        }
        viewModelScope.launch {
            try {
                // В Firebase используем email. Если пользователь ввел просто логин,
                // можно превратить его в подобие email или требовать email.
                // Для простоты предположим, что username - это email.
                val email = if (username.value.contains("@")) username.value else "${username.value}@kinetic.app"
                
                val result = auth.createUserWithEmailAndPassword(email, password.value).await()
                val firebaseUser = result.user
                
                if (firebaseUser != null) {
                    val newUserId = userDao.registerUser(
                        UserEntity(
                            username = username.value,
                            passwordHash = password.value,
                            firebaseUid = firebaseUser.uid
                        )
                    )
                    sessionManager.saveUserId(newUserId)
                    _authSuccess.emit(true)
                }
            } catch (e: Exception) {
                authError.value = "Ошибка регистрации: ${e.localizedMessage}"
            }
        }
    }

    fun login() {
        if (username.value.isBlank() || password.value.isBlank()) {
            authError.value = "Заполните все поля"
            return
        }
        viewModelScope.launch {
            try {
                val email = if (username.value.contains("@")) username.value else "${username.value}@kinetic.app"
                
                auth.signInWithEmailAndPassword(email, password.value).await()
                
                val user = userDao.getUserByUsername(username.value)
                if (user != null) {
                    sessionManager.saveUserId(user.id)
                }
                _authSuccess.emit(true)
            } catch (e: Exception) {
                authError.value = "Ошибка входа: ${e.localizedMessage}"
            }
        }
    }
}