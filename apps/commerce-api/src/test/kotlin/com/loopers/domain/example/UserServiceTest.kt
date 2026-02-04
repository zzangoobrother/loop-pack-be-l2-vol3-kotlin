package com.loopers.domain.example

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
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

    @org.junit.jupiter.api.BeforeEach
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

        @DisplayName("비밀번호가 8자 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordLessThan8Chars() {
            // arrange
            val loginId = "abcde12345"
            val password = "Ab1!@#$"
            val name = "홍길동"
            val email = "abcde@gmail.com"
            val birthday = LocalDate.of(1980, 1, 1)

            whenever(userRepository.existsByLoginId(loginId)).thenReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                userService.signUp(loginId, password, name, email, birthday)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호가 16자 초과이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordMoreThan16Chars() {
            // arrange
            val loginId = "abcde12345"
            val password = "Abcd1234!@#$12345"
            val name = "홍길동"
            val email = "abcde@gmail.com"
            val birthday = LocalDate.of(1980, 1, 1)

            whenever(userRepository.existsByLoginId(loginId)).thenReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                userService.signUp(loginId, password, name, email, birthday)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 영문 대소문자가 없으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordNoLetters() {
            // arrange
            val loginId = "abcde12345"
            val password = "12345678!@#$"
            val name = "홍길동"
            val email = "abcde@gmail.com"
            val birthday = LocalDate.of(1980, 1, 1)

            whenever(userRepository.existsByLoginId(loginId)).thenReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                userService.signUp(loginId, password, name, email, birthday)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 숫자가 없으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordNoDigits() {
            // arrange
            val loginId = "abcde12345"
            val password = "Abcdabcd!@#$"
            val name = "홍길동"
            val email = "abcde@gmail.com"
            val birthday = LocalDate.of(1980, 1, 1)

            whenever(userRepository.existsByLoginId(loginId)).thenReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                userService.signUp(loginId, password, name, email, birthday)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 특수문자가 없으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordNoSpecialChars() {
            // arrange
            val loginId = "abcde12345"
            val password = "Abcd12345678"
            val name = "홍길동"
            val email = "abcde@gmail.com"
            val birthday = LocalDate.of(1980, 1, 1)

            whenever(userRepository.existsByLoginId(loginId)).thenReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                userService.signUp(loginId, password, name, email, birthday)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 생년월일이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordContainsBirthday() {
            // arrange
            val loginId = "abcde12345"
            val password = "Abcd19800101!@"
            val name = "홍길동"
            val email = "abcde@gmail.com"
            val birthday = LocalDate.of(1980, 1, 1)

            whenever(userRepository.existsByLoginId(loginId)).thenReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                userService.signUp(loginId, password, name, email, birthday)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
