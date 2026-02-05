package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MaskedNameTest {

    @DisplayName("MaskedName 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("3글자 이름이 주어지면, 마지막 글자를 *로 치환한다.")
        @Test
        fun masksLastCharacter_whenThreeCharacterNameProvided() {
            // arrange
            val name = "홍길동"

            // act
            val maskedName = MaskedName.from(name)

            // assert
            assertThat(maskedName.value).isEqualTo("홍길*")
        }

        @DisplayName("2글자 이름이 주어지면, 마지막 글자를 *로 치환한다.")
        @Test
        fun masksLastCharacter_whenTwoCharacterNameProvided() {
            // arrange
            val name = "김수"

            // act
            val maskedName = MaskedName.from(name)

            // assert
            assertThat(maskedName.value).isEqualTo("김*")
        }

        @DisplayName("1글자 이름이 주어지면, *로 치환한다.")
        @Test
        fun masksEntireName_whenSingleCharacterNameProvided() {
            // arrange
            val name = "A"

            // act
            val maskedName = MaskedName.from(name)

            // assert
            assertThat(maskedName.value).isEqualTo("*")
        }

        @DisplayName("빈 문자열이 주어지면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenEmptyNameProvided() {
            // arrange
            val name = ""

            // act
            val exception = assertThrows<CoreException> {
                MaskedName.from(name)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
