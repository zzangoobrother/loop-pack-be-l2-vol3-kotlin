package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository, passwordEncoder)
    }

    @DisplayName("회원가입 할 때,")
    @Nested
    inner class SignUp {
        @DisplayName("유효한 회원 정보를 전달하면, 회원이 생성된다.")
        @Test
        fun createsUser_whenValidUserInfoIsProvided() {
            // arrange
            val loginId = "abcde12345"
            val password = "Abcd1234!@#$"
            val name = "홍길동"
            val email = "abcde@gmail.com"
            val birthday = LocalDate.of(1980, 1, 1)
            val encodedPassword = "encoded_password"

            whenever(userRepository.existsByLoginId(loginId)).thenReturn(false)
            whenever(passwordEncoder.encode(password)).thenReturn(encodedPassword)
            whenever(userRepository.save(any())).thenAnswer { invocation ->
                invocation.getArgument<User>(0)
            }

            // act
            val result = userService.signUp(loginId, password, name, email, birthday)

            // assert
            assertAll(
                { assertThat(result).isNotNull() },
                { assertThat(result.loginId).isEqualTo(loginId) },
                { assertThat(result.name).isEqualTo(name) },
                { assertThat(result.email).isEqualTo(email) },
                { assertThat(result.birthday).isEqualTo(birthday) },
            )
        }

        @DisplayName("이미 존재하는 로그인 ID로 가입하면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenLoginIdAlreadyExists() {
            // arrange
            val loginId = "existingUser"
            val password = "Abcd1234!@#$"
            val name = "홍길동"
            val email = "abcde@gmail.com"
            val birthday = LocalDate.of(1980, 1, 1)

            whenever(userRepository.existsByLoginId(loginId)).thenReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                userService.signUp(loginId, password, name, email, birthday)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("비밀번호는 암호화되어 저장된다.")
        @Test
        fun encryptsPassword_whenSignUp() {
            // arrange
            val loginId = "abcde12345"
            val password = "Abcd1234!@#$"
            val name = "홍길동"
            val email = "abcde@gmail.com"
            val birthday = LocalDate.of(1980, 1, 1)
            val encodedPassword = "encoded_password_hash"

            whenever(userRepository.existsByLoginId(loginId)).thenReturn(false)
            whenever(passwordEncoder.encode(password)).thenReturn(encodedPassword)
            whenever(userRepository.save(any())).thenAnswer { invocation ->
                invocation.getArgument<User>(0)
            }

            // act
            val result = userService.signUp(loginId, password, name, email, birthday)

            // assert
            assertThat(result.password).isEqualTo(encodedPassword)
        }
    }

    @DisplayName("인증 할 때,")
    @Nested
    inner class Authenticate {
        @DisplayName("유효한 로그인 ID와 비밀번호를 전달하면, 사용자를 반환한다.")
        @Test
        fun returnsUser_whenValidCredentialsAreProvided() {
            // arrange
            val loginId = "testuser"
            val password = "Abcd1234!@#$"
            val encodedPassword = "encoded_password"
            val user = User(
                loginId = loginId,
                password = encodedPassword,
                name = "홍길동",
                email = "test@example.com",
                birthday = LocalDate.of(1990, 1, 1),
            )

            whenever(userRepository.findByLoginId(loginId)).thenReturn(user)
            whenever(passwordEncoder.matches(password, encodedPassword)).thenReturn(true)

            // act
            val result = userService.authenticate(loginId, password)

            // assert
            assertAll(
                { assertThat(result).isNotNull() },
                { assertThat(result.loginId).isEqualTo(loginId) },
            )
        }

        @DisplayName("존재하지 않는 로그인 ID를 전달하면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorized_whenLoginIdNotFound() {
            // arrange
            val loginId = "nonexistent"
            val password = "Abcd1234!@#$"

            whenever(userRepository.findByLoginId(loginId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                userService.authenticate(loginId, password)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @DisplayName("비밀번호가 일치하지 않으면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorized_whenPasswordNotMatches() {
            // arrange
            val loginId = "testuser"
            val password = "WrongPassword1!"
            val encodedPassword = "encoded_password"
            val user = User(
                loginId = loginId,
                password = encodedPassword,
                name = "홍길동",
                email = "test@example.com",
                birthday = LocalDate.of(1990, 1, 1),
            )

            whenever(userRepository.findByLoginId(loginId)).thenReturn(user)
            whenever(passwordEncoder.matches(password, encodedPassword)).thenReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                userService.authenticate(loginId, password)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }
    }

    @DisplayName("비밀번호 변경할 때,")
    @Nested
    inner class ChangePassword {
        private val loginId = "testuser"
        private val encodedCurrentPassword = "encoded_current_password"
        private val user = User(
            loginId = loginId,
            password = encodedCurrentPassword,
            name = "홍길동",
            email = "test@example.com",
            birthday = LocalDate.of(1990, 5, 15),
        )

        @DisplayName("사용자가 존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenUserNotExists() {
            // arrange
            val userId = 1L
            val currentPassword = "CurrentPassword1!"
            val newPassword = "NewPassword1!"

            whenever(userRepository.find(userId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(userId, currentPassword, newPassword)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("현재 비밀번호가 일치하지 않으면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorized_whenCurrentPasswordNotMatches() {
            // arrange
            val wrongCurrentPassword = "WrongPassword1!"
            val newPassword = "NewPassword1!"

            whenever(userRepository.find(user.id)).thenReturn(user)
            whenever(passwordEncoder.matches(wrongCurrentPassword, encodedCurrentPassword)).thenReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(user.id, wrongCurrentPassword, newPassword)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @DisplayName("현재 비밀번호와 새 비밀번호가 동일하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNewPasswordSameAsCurrent() {
            // arrange
            val currentPassword = "CurrentPassword1!"

            whenever(userRepository.find(user.id)).thenReturn(user)
            whenever(passwordEncoder.matches(currentPassword, encodedCurrentPassword)).thenReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(user.id, currentPassword, currentPassword)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("새 비밀번호 형식이 올바르지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNewPasswordFormatInvalid() {
            // arrange
            val currentPassword = "CurrentPassword1!"
            val invalidNewPassword = "weak"

            whenever(userRepository.find(user.id)).thenReturn(user)
            whenever(passwordEncoder.matches(currentPassword, encodedCurrentPassword)).thenReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(user.id, currentPassword, invalidNewPassword)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("새 비밀번호에 생년월일이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNewPasswordContainsBirthday() {
            // arrange
            val currentPassword = "CurrentPassword1!"
            val newPasswordWithBirthday = "Pass19900515!@"

            whenever(userRepository.find(user.id)).thenReturn(user)
            whenever(passwordEncoder.matches(currentPassword, encodedCurrentPassword)).thenReturn(true)

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(user.id, currentPassword, newPasswordWithBirthday)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("유효한 현재 비밀번호와 새 비밀번호를 전달하면, 비밀번호가 변경된다.")
        @Test
        fun changesPassword_whenValidPasswordsProvided() {
            // arrange
            val currentPassword = "CurrentPassword1!"
            val newPassword = "NewPassword1!"
            val encodedNewPassword = "encoded_new_password"

            whenever(userRepository.find(user.id)).thenReturn(user)
            whenever(passwordEncoder.matches(currentPassword, encodedCurrentPassword)).thenReturn(true)
            whenever(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword)

            // act
            userService.changePassword(user.id, currentPassword, newPassword)

            // assert
            assertThat(user.password).isEqualTo(encodedNewPassword)
        }
    }
}
