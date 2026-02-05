package com.loopers.interfaces.api.user

import com.loopers.application.user.UserInfo
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.time.LocalDate

class UserDto {
    data class SignUpRequest(
        @field:NotBlank(message = "로그인 ID는 필수입니다.")
        @field:Pattern(regexp = "^[a-zA-Z0-9]+$", message = "로그인 ID는 영문과 숫자만 사용할 수 있습니다.")
        val loginId: String,
        @field:NotBlank(message = "비밀번호는 필수입니다.")
        val password: String,
        @field:NotBlank(message = "이름은 필수입니다.")
        val name: String,
        @field:Email(message = "올바른 이메일 형식이 아닙니다.")
        @field:NotBlank(message = "이메일은 필수입니다.")
        val email: String,
        val birthday: LocalDate,
    )

    data class SignUpResponse(
        val id: Long,
        val loginId: String,
        val name: String,
        val email: String,
    ) {
        companion object {
            fun from(info: UserInfo): SignUpResponse {
                return SignUpResponse(
                    id = info.id,
                    loginId = info.loginId,
                    name = info.name,
                    email = info.email,
                )
            }
        }
    }

    data class MeResponse(
        val loginId: String,
        val name: String,
        val email: String,
        val birthday: LocalDate?,
    ) {
        companion object {
            fun from(info: UserInfo): MeResponse {
                return MeResponse(
                    loginId = info.loginId,
                    name = info.maskedName ?: info.name,
                    email = info.email,
                    birthday = info.birthday,
                )
            }
        }
    }

    data class ChangePasswordRequest(
        val currentPassword: String,
        val newPassword: String,
    )
}
