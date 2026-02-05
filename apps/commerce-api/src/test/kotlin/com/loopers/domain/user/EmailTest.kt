package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EmailTest {

    @DisplayName("Email 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("유효한 이메일이 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsEmail_whenValidEmailProvided() {
            // arrange
            val validEmail = "test@example.com"

            // act
            val email = Email.of(validEmail)

            // assert
            assertThat(email.value).isEqualTo(validEmail)
        }

        @DisplayName("이메일이 빈 문자열이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenEmailIsBlank() {
            // arrange
            val blankEmail = "   "

            // act
            val exception = assertThrows<CoreException> {
                Email.of(blankEmail)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이메일 형식이 올바르지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenEmailFormatIsInvalid() {
            // arrange
            val invalidEmail = "invalid-email"

            // act
            val exception = assertThrows<CoreException> {
                Email.of(invalidEmail)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이메일에 @가 없으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenEmailMissingAtSign() {
            // arrange
            val invalidEmail = "testexample.com"

            // act
            val exception = assertThrows<CoreException> {
                Email.of(invalidEmail)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이메일 도메인이 올바르지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenEmailDomainIsInvalid() {
            // arrange
            val invalidEmail = "test@.com"

            // act
            val exception = assertThrows<CoreException> {
                Email.of(invalidEmail)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
