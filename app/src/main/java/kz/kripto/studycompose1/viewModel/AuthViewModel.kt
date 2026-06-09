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

/**
 * Моя ViewModel для авторизации и регистрации.
 * Она "дирижирует" процессом входа: общается с Firebase и сохраняет данные в телефон.
 */
class AuthViewModel(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) : ViewModel() {

    // Поля ввода для формы (двусторонняя связь с экраном)
    var username = mutableStateOf("")
    var email = mutableStateOf("")
    var password = mutableStateOf("")

    // Сюда пишу текст ошибки, если что-то пошло не так (например, пароль короткий)
    var authError = mutableStateOf<String?>(null)

    // Специальный канал, чтобы сказать экрану: "Всё ок, можно переходить на главную!"
    private val _authSuccess = MutableSharedFlow<Boolean>()
    val authSuccess = _authSuccess.asSharedFlow()

    /**
     * Создаю новый аккаунт. 
     * Процесс: Проверка имени -> Регистрация в Firebase -> Сохранение в Room -> Сохранение в Firestore.
     */
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
                // 1. Проверяю в облаке, не занял ли кто-то это имя раньше нас
                if (userRepository.isUsernameTaken(cleanUsername)) {
                    authError.value = "Это имя пользователя уже занято"
                    return@launch
                }

                // 2. Регистрирую почту и пароль в Firebase Auth
                val result = auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword).await()
                val firebaseUser = result.user
                
                if (firebaseUser != null) {
                    val newUser = UserEntity(
                        username = cleanUsername,
                        email = cleanEmail,
                        passwordHash = cleanPassword,
                        firebaseUid = firebaseUser.uid
                    )
                    // 3. Записываю нового пользователя в локальную базу телефона
                    val newUserId = userDao.registerUser(newUser)
                    
                    // 4. Отправляю профиль в Firestore, чтобы другие видели наш логин
                    userRepository.saveUserToFirestore(newUser.copy(id = newUserId))
                    
                    // 5. Сохраняю сессию, чтобы не логиниться при каждом запуске
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

    /**
     * Вход в уже созданный аккаунт.
     * Процесс: Firebase Login -> Синхронизация профиля -> Сохранение сессии.
     */
    fun login() {
        val cleanEmail = email.value.trim()
        val cleanPassword = password.value.trim()

        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            authError.value = "Заполните все поля"
            return
        }
        viewModelScope.launch {
            try {
                // 1. Спрашиваю у Firebase, верны ли почта и пароль
                auth.signInWithEmailAndPassword(cleanEmail, cleanPassword).await()
                
                // 2. Если пустили — скачиваю данные профиля (логин и т.д.) из облака в телефон
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
