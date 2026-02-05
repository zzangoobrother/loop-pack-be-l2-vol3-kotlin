package com.loopers.interfaces.api.user

import com.loopers.domain.user.User
import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "User API", description = "회원 API")
interface UserApiSpec {
    @Operation(
        summary = "회원가입",
        description = "새로운 회원을 등록합니다.",
    )
    fun signUp(request: UserDto.SignUpRequest): ApiResponse<UserDto.SignUpResponse>

    @Operation(
        summary = "내 정보 조회",
        description = "인증된 사용자의 정보를 조회합니다.",
    )
    fun getMe(user: User): ApiResponse<UserDto.MeResponse>

    @Operation(
        summary = "비밀번호 변경",
        description = "현재 비밀번호를 확인 후 새 비밀번호로 변경합니다.",
    )
    fun changePassword(user: User, request: UserDto.ChangePasswordRequest): ApiResponse<Unit>
}
