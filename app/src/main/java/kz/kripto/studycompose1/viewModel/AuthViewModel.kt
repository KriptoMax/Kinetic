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
import kz.kripto.studycompose1.repository.UserRepository

// Моя ViewModel для авторизации и регистрации через Firebase
class AuthViewModel(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) : ViewModel() {

    // Поля ввода для формы
    var username = mutableStateOf("")
    var email = mutableStateOf("")
    var password = mutableStateOf("")

    // Текст ошибки, если что-то пошло не так
    var authError = mutableStateOf<String?>(null)

    // Поток для уведомления экрана об успешном входе
    private val _authSuccess = MutableSharedFlow<Boolean>()
    val authSuccess = _authSuccess.asSharedFlow()

    // Регистрация нового пользователя
    fun register() {
        val cleanUsername = username.value.trim()
        val cleanEmail = email.value.trim()
        val cleanPassword = password.value.trim()

        if (cleanUsername.isBlank() || cleanEmail.isBlank() || cleanPassword.isBlank()) {
            authError.value = "Заполните все поля"
            return
        }
        viewModelScope.launch {
            try {
                // 1. Проверяю в облаке, свободен ли логин
                if (userRepository.isUsernameTaken(cleanUsername)) {
                    authError.value = "Это имя пользователя уже занято"
                    return@launch
                }

                // 2. Регистрирую в Firebase Auth (по почте и паролю)
                val result = auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword).await()
                val firebaseUser = result.user
                
                if (firebaseUser != null) {
                    val newUser = UserEntity(
                        username = cleanUsername,
                        email = cleanEmail,
                        passwordHash = cleanPassword,
                        firebaseUid = firebaseUser.uid
                    )
                    // 3. Сохраняю в локальную базу Room
                    val newUserId = userDao.registerUser(newUser)
                    
                    // 4. Сохраняю расширенный профиль в Firestore
                    userRepository.saveUserToFirestore(newUser.copy(id = newUserId))
                    
                    // 5. Запоминаю сессию локально
                    sessionManager.saveUserId(newUserId)
                    sessionManager.saveUsername(cleanUsername)
                    _authSuccess.emit(true)
                }
            } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                authError.value = "Этот Email уже используется"
            } catch (e: Exception) {
                authError.value = "Ошибка регистрации: ${e.localizedMessage}"
            }
        }
    }

    // Вход в существующий аккаунт
    fun login() {
        val cleanEmail = email.value.trim()
        val cleanPassword = password.value.trim()

        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            authError.value = "Заполните все поля"
            return
        }
        viewModelScope.launch {
            try {
                // 1. Пытаюсь войти через Firebase
                auth.signInWithEmailAndPassword(cleanEmail, cleanPassword).await()
                
                // 2. Если вошел, скачиваю профиль из облака в телефон
                val userId = userRepository.syncUserAfterLogin()
                
                if (userId != -1L) {
                    val user = userDao.getUserByFirebaseUid(auth.currentUser?.uid ?: "")
                    sessionManager.saveUserId(userId)
                    sessionManager.saveUsername(user?.username ?: "User")
                    _authSuccess.emit(true)
                } else {
                    authError.value = "Ошибка загрузки профиля"
                }
            } catch (e: Exception) {
                authError.value = "Ошибка входа: ${e.localizedMessage}"
            }
        }
    }
}
