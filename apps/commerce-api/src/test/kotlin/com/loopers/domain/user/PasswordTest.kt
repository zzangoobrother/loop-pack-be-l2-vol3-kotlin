package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class PasswordTest {

    @DisplayName("Password 생성할 때,")
    @Nested
    inner class Create {
        private val validPassword = "Abcd1234!@"
        private val birthday = LocalDate.of(1990, 5, 15)

        @DisplayName("유효한 비밀번호와 생년월일이 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsPassword_whenValidPasswordAndBirthdayProvided() {
            // arrange & act
            val password = Password.of(validPassword, birthday)

            // assert
            assertThat(password.value).isEqualTo(validPassword)
        }

        @DisplayName("비밀번호가 8자 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordLessThan8Chars() {
            // arrange
            val shortPassword = "Ab1!@#$"

            // act
            val exception = assertThrows<CoreException> {
                Password.of(shortPassword, birthday)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호가 16자 초과이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordMoreThan16Chars() {
            // arrange
            val longPassword = "Abcd1234!@#$12345"

            // act
            val exception = assertThrows<CoreException> {
                Password.of(longPassword, birthday)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 소문자가 없으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordNoLowercase() {
            // arrange
            val noLowercasePassword = "ABCD1234!@"

            // act
            val exception = assertThrows<CoreException> {
                Password.of(noLowercasePassword, birthday)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 대문자가 없으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordNoUppercase() {
            // arrange
            val noUppercasePassword = "abcd1234!@"

            // act
            val exception = assertThrows<CoreException> {
                Password.of(noUppercasePassword, birthday)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 숫자가 없으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordNoDigits() {
            // arrange
            val noDigitsPassword = "Abcdabcd!@#$"

            // act
            val exception = assertThrows<CoreException> {
                Password.of(noDigitsPassword, birthday)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 특수문자가 없으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordNoSpecialChars() {
            // arrange
            val noSpecialPassword = "Abcd12345678"

            // act
            val exception = assertThrows<CoreException> {
                Password.of(noSpecialPassword, birthday)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 생년월일(YYYYMMDD)이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordContainsBirthday() {
            // arrange
            val birthdayPassword = "Abcd19900515!@"
            val testBirthday = LocalDate.of(1990, 5, 15)

            // act
            val exception = assertThrows<CoreException> {
                Password.of(birthdayPassword, testBirthday)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
