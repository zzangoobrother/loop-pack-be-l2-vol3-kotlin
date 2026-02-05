package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class LoginId private constructor(
    val value: String,
) {
    companion object {
        private val LOGIN_ID_PATTERN = Regex("^[a-zA-Z0-9]+$")

        fun of(value: String): LoginId {
            validate(value)
            return LoginId(value)
        }

        private fun validate(value: String) {
            if (value.isBlank()) {
                throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 비어있을 수 없습니다.")
            }
            if (!LOGIN_ID_PATTERN.matches(value)) {
                throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 영문과 숫자만 사용할 수 있습니다.")
            }
        }
    }
}
