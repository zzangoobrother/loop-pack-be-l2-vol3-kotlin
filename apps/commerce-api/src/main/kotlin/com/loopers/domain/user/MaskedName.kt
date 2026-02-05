package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class MaskedName private constructor(
    val value: String,
) {
    companion object {
        fun from(name: String): MaskedName {
            if (name.isEmpty()) {
                throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.")
            }
            val masked = name.dropLast(1) + "*"
            return MaskedName(masked)
        }
    }
}
