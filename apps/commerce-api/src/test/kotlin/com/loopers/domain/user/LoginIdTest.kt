package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LoginIdTest {

    @DisplayName("LoginId 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("유효한 로그인 ID가 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsLoginId_whenValidLoginIdProvided() {
            // arrange
            val validLoginId = "testuser123"

            // act
            val loginId = LoginId.of(validLoginId)

            // assert
            assertThat(loginId.value).isEqualTo(validLoginId)
        }

        @DisplayName("로그인 ID가 빈 문자열이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenLoginIdIsBlank() {
            // arrange
            val blankLoginId = "   "

            // act
            val exception = assertThrows<CoreException> {
                LoginId.of(blankLoginId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("로그인 ID에 특수문자가 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenLoginIdContainsSpecialCharacters() {
            // arrange
            val invalidLoginId = "test@user!"

            // act
            val exception = assertThrows<CoreException> {
                LoginId.of(invalidLoginId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("로그인 ID에 한글이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenLoginIdContainsKorean() {
            // arrange
            val invalidLoginId = "test한글123"

            // act
            val exception = assertThrows<CoreException> {
                LoginId.of(invalidLoginId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
