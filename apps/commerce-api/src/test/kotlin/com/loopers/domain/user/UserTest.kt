package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class UserTest {

    @DisplayName("비밀번호 변경할 때,")
    @Nested
    inner class ChangePassword {
        private val loginId = "testuser1"
        private val currentPassword = "encodedPassword123"
        private val name = "홍길동"
        private val email = "test@example.com"
        private val birthday = LocalDate.of(1990, 5, 15)

        @DisplayName("현재 비밀번호와 새 비밀번호가 동일하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNewPasswordSameAsCurrent() {
            // arrange
            val user = User(
                loginId = loginId,
                password = currentPassword,
                name = name,
                email = email,
                birthday = birthday,
            )
            val samePassword = currentPassword

            // act
            val exception = assertThrows<CoreException> {
                user.changePassword(samePassword)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("회원 생성할 때, 정상적으로 생성된다.")
    @Nested
    inner class Create {
        private val loginId = "abcde12345"
        private val password = "Abcd1234abcd!@#$"
        private val name = "홍길동"
        private val email = "abcde@gmail.com"
        private val birthday = LocalDate.of(1980, 1, 1)

        @DisplayName("회원 데이터가 모두 주어지면, 정상적으로 생성된다.")
        @Test
        fun createUser() {
            val user = User(loginId = loginId, password = password, name = name, email = email, birthday = birthday)

            assertAll(
                { assertThat(user.loginId).isEqualTo(loginId) },
                { assertThat(user.password).isEqualTo(password) },
                { assertThat(user.name).isEqualTo(name) },
                { assertThat(user.email).isEqualTo(email) },
                { assertThat(user.birthday).isEqualTo(birthday) },
            )
        }

        @DisplayName("name 빈칸으로만 이루어져 있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenNameIsBlank() {
            val blankName = "  "

            val result = assertThrows<CoreException> {
                User(loginId = loginId, password = password, name = blankName, email = email, birthday = birthday)
            }

            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
