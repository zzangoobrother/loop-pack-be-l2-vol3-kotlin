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
        private val currentEncodedPassword = "encoded_current_password"
        private val name = "홍길동"
        private val email = "test@example.com"
        private val birthday = LocalDate.of(1990, 5, 15)

        @DisplayName("새 비밀번호로 변경하면, 비밀번호가 변경된다.")
        @Test
        fun changesPassword_whenNewPasswordProvided() {
            // arrange
            val newEncodedPassword = "encoded_new_password"
            val user = User(
                loginId = loginId,
                password = currentEncodedPassword,
                name = name,
                email = email,
                birthday = birthday,
            )

            // act
            user.changePassword(newEncodedPassword)

            // assert
            assertThat(user.password).isEqualTo(newEncodedPassword)
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
