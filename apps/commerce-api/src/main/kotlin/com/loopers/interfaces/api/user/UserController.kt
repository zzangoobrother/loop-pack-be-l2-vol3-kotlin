package com.loopers.interfaces.api.user

import com.loopers.application.user.UserFacade
import com.loopers.domain.user.User
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.auth.AuthenticatedUser
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userFacade: UserFacade,
) : UserApiSpec {

    @PostMapping("/signup")
    override fun signUp(
        @RequestBody @Valid request: UserDto.SignUpRequest,
    ): ApiResponse<UserDto.SignUpResponse> {
        return userFacade.signUp(
            loginId = request.loginId,
            password = request.password,
            name = request.name,
            email = request.email,
            birthday = request.birthday,
        )
            .let { UserDto.SignUpResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/me")
    override fun getMe(
        @AuthenticatedUser user: User,
    ): ApiResponse<UserDto.MeResponse> {
        return userFacade.getMe(user)
            .let { UserDto.MeResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
