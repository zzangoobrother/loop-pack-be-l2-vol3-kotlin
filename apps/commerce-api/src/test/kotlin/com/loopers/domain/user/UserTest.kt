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

    @DisplayName("회원 생성할 때, 정상적으로 생성된다.")
    @Nested
    inner class Create {
        var loginId = "abcde12345"
        var password = "Abcd1234abcd!@#$"
        var name = "홍길동"
        var email = "abcde@gmail.com"
        var birthday = LocalDate.of(1980, 1, 1)

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
