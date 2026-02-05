package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class UserServiceIntegrationTest @Autowired constructor(
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("회원가입 할 때,")
    @Nested
    inner class SignUp {
        @DisplayName("유효한 회원 정보를 전달하면, 회원이 생성되고 DB에 저장된다.")
        @Test
        fun createsUserAndSavesToDb_whenValidUserInfoIsProvided() {
            // arrange
            val loginId = "testuser123"
            val password = "Test1234!@"
            val name = "홍길동"
            val email = "test@example.com"
            val birthday = LocalDate.of(1990, 1, 15)

            // act
            val result = userService.signUp(loginId, password, name, email, birthday)

            // assert
            val savedUser = userRepository.find(result.id)
            assertAll(
                { assertThat(savedUser).isNotNull() },
                { assertThat(savedUser?.loginId).isEqualTo(loginId) },
                { assertThat(savedUser?.name).isEqualTo(name) },
                { assertThat(savedUser?.email).isEqualTo(email) },
                { assertThat(savedUser?.birthday).isEqualTo(birthday) },
            )
        }

        @DisplayName("비밀번호가 암호화되어 저장된다.")
        @Test
        fun encryptsPasswordAndSavesToDb_whenSignUp() {
            // arrange
            val loginId = "testuser123"
            val password = "Test1234!@"
            val name = "홍길동"
            val email = "test@example.com"
            val birthday = LocalDate.of(1990, 1, 15)

            // act
            val result = userService.signUp(loginId, password, name, email, birthday)

            // assert
            val savedUser = userRepository.find(result.id)
            assertAll(
                { assertThat(savedUser?.password).isNotEqualTo(password) },
                { assertThat(passwordEncoder.matches(password, savedUser!!.password)).isTrue() },
            )
        }

        @DisplayName("이미 존재하는 로그인 ID로 가입하면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenLoginIdAlreadyExists() {
            // arrange
            val loginId = "existinguser"
            val password = "Test1234!@"
            val name = "홍길동"
            val email = "test@example.com"
            val birthday = LocalDate.of(1990, 1, 15)

            userService.signUp(loginId, password, name, email, birthday)

            // act
            val exception = assertThrows<CoreException> {
                userService.signUp(loginId, "NewPass1234!@", "김철수", "new@example.com", LocalDate.of(1985, 5, 20))
            }

            // assert
            assertAll(
                { assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT) },
                { assertThat(exception.message).contains("이미 존재하는 로그인 ID") },
            )
        }

        @DisplayName("비밀번호에 생년월일이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordContainsBirthday() {
            // arrange
            val loginId = "testuser123"
            val birthday = LocalDate.of(1990, 1, 15)
            val passwordWithBirthday = "Pass19900115!@"
            val name = "홍길동"
            val email = "test@example.com"

            // act
            val exception = assertThrows<CoreException> {
                userService.signUp(loginId, passwordWithBirthday, name, email, birthday)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("인증 할 때,")
    @Nested
    inner class Authenticate {
        @DisplayName("유효한 로그인 ID와 비밀번호를 전달하면, 사용자를 반환한다.")
        @Test
        fun returnsUser_whenValidCredentialsAreProvided() {
            // arrange
            val loginId = "testuser123"
            val password = "Test1234!@"
            val name = "홍길동"
            val email = "test@example.com"
            val birthday = LocalDate.of(1990, 1, 15)

            userService.signUp(loginId, password, name, email, birthday)

            // act
            val result = userService.authenticate(loginId, password)

            // assert
            assertAll(
                { assertThat(result).isNotNull() },
                { assertThat(result.loginId).isEqualTo(loginId) },
                { assertThat(result.name).isEqualTo(name) },
            )
        }

        @DisplayName("존재하지 않는 로그인 ID를 전달하면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorized_whenLoginIdNotFound() {
            // arrange
            val loginId = "nonexistent"
            val password = "Test1234!@"

            // act
            val exception = assertThrows<CoreException> {
                userService.authenticate(loginId, password)
            }

            // assert
            assertAll(
                { assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED) },
                { assertThat(exception.message).contains("존재하지 않는 사용자") },
            )
        }

        @DisplayName("비밀번호가 일치하지 않으면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorized_whenPasswordNotMatches() {
            // arrange
            val loginId = "testuser123"
            val password = "Test1234!@"
            val wrongPassword = "WrongPass1234!@"

            userService.signUp(loginId, password, "홍길동", "test@example.com", LocalDate.of(1990, 1, 15))

            // act
            val exception = assertThrows<CoreException> {
                userService.authenticate(loginId, wrongPassword)
            }

            // assert
            assertAll(
                { assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED) },
                { assertThat(exception.message).contains("비밀번호가 일치하지 않습니다") },
            )
        }
    }

    @DisplayName("비밀번호 변경 할 때,")
    @Nested
    inner class ChangePassword {
        @DisplayName("유효한 현재 비밀번호와 새 비밀번호를 전달하면, 비밀번호가 변경된다.")
        @Test
        fun changesPassword_whenValidPasswordsProvided() {
            // arrange
            val loginId = "testuser123"
            val currentPassword = "Test1234!@"
            val newPassword = "NewPass1234!@"
            val birthday = LocalDate.of(1990, 1, 15)

            val user = userService.signUp(loginId, currentPassword, "홍길동", "test@example.com", birthday)

            // act
            userService.changePassword(user.id, currentPassword, newPassword)

            // assert
            val updatedUser = userRepository.find(user.id)
            assertAll(
                { assertThat(passwordEncoder.matches(newPassword, updatedUser!!.password)).isTrue() },
                { assertThat(passwordEncoder.matches(currentPassword, updatedUser!!.password)).isFalse() },
            )
        }

        @DisplayName("변경된 비밀번호로 인증이 가능하다.")
        @Test
        fun canAuthenticateWithNewPassword_afterPasswordChange() {
            // arrange
            val loginId = "testuser123"
            val currentPassword = "Test1234!@"
            val newPassword = "NewPass1234!@"
            val birthday = LocalDate.of(1990, 1, 15)

            val user = userService.signUp(loginId, currentPassword, "홍길동", "test@example.com", birthday)
            userService.changePassword(user.id, currentPassword, newPassword)

            // act
            val result = userService.authenticate(loginId, newPassword)

            // assert
            assertThat(result.loginId).isEqualTo(loginId)
        }

        @DisplayName("존재하지 않는 사용자 ID를 전달하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenUserNotExists() {
            // arrange
            val nonExistentUserId = 9999L
            val currentPassword = "Test1234!@"
            val newPassword = "NewPass1234!@"

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(nonExistentUserId, currentPassword, newPassword)
            }

            // assert
            assertAll(
                { assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND) },
                { assertThat(exception.message).contains("사용자를 찾을 수 없습니다") },
            )
        }

        @DisplayName("현재 비밀번호가 일치하지 않으면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorized_whenCurrentPasswordNotMatches() {
            // arrange
            val loginId = "testuser123"
            val currentPassword = "Test1234!@"
            val wrongCurrentPassword = "WrongPass1234!@"
            val newPassword = "NewPass1234!@"
            val birthday = LocalDate.of(1990, 1, 15)

            val user = userService.signUp(loginId, currentPassword, "홍길동", "test@example.com", birthday)

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
            val loginId = "testuser123"
            val currentPassword = "Test1234!@"
            val birthday = LocalDate.of(1990, 1, 15)

            val user = userService.signUp(loginId, currentPassword, "홍길동", "test@example.com", birthday)

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(user.id, currentPassword, currentPassword)
            }

            // assert
            assertAll(
                { assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST) },
                { assertThat(exception.message).contains("새 비밀번호는 현재 비밀번호와 달라야 합니다") },
            )
        }

        @DisplayName("새 비밀번호에 생년월일이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNewPasswordContainsBirthday() {
            // arrange
            val loginId = "testuser123"
            val currentPassword = "Test1234!@"
            val birthday = LocalDate.of(1990, 1, 15)
            val newPasswordWithBirthday = "Pass19900115!@"

            val user = userService.signUp(loginId, currentPassword, "홍길동", "test@example.com", birthday)

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(user.id, currentPassword, newPasswordWithBirthday)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("새 비밀번호 형식이 올바르지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNewPasswordFormatInvalid() {
            // arrange
            val loginId = "testuser123"
            val currentPassword = "Test1234!@"
            val invalidNewPassword = "weak"
            val birthday = LocalDate.of(1990, 1, 15)

            val user = userService.signUp(loginId, currentPassword, "홍길동", "test@example.com", birthday)

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(user.id, currentPassword, invalidNewPassword)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
