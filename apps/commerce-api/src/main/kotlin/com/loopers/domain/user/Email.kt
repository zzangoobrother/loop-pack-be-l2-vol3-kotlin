package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class Email private constructor(
    val value: String,
) {
    companion object {
        private val EMAIL_PATTERN = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")

        fun of(value: String): Email {
            validate(value)
            return Email(value)
        }

        private fun validate(value: String) {
            if (value.isBlank()) {
                throw CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.")
            }
            if (!EMAIL_PATTERN.matches(value)) {
                throw CoreException(ErrorType.BAD_REQUEST, "올바른 이메일 형식이 아닙니다.")
            }
        }
    }
}
