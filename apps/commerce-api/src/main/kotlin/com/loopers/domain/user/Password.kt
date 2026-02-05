package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.LocalDate

class Password private constructor(
    val value: String,
) {
    companion object {
        private val PASSWORD_PATTERN =
            Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&#])[A-Za-z\\d@\$!%*?&#]{8,16}$")

        fun of(rawPassword: String, birthday: LocalDate): Password {
            validateFormat(rawPassword)
            validateNotContainsBirthday(rawPassword, birthday)
            return Password(rawPassword)
        }

        private fun validateFormat(password: String) {
            if (!PASSWORD_PATTERN.matches(password)) {
                throw CoreException(
                    ErrorType.BAD_REQUEST,
                    "비밀번호는 8~16자의 영문 대소문자, 숫자, 특수문자를 포함해야 합니다.",
                )
            }
        }

        private fun validateNotContainsBirthday(password: String, birthday: LocalDate) {
            val birthdayString = birthday.toString().replace("-", "")
            if (password.contains(birthdayString)) {
                throw CoreException(
                    ErrorType.BAD_REQUEST,
                    "비밀번호에 생년월일을 포함할 수 없습니다.",
                )
            }
        }
    }
}
