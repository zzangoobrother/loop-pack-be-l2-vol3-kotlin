package com.loopers.application.user

import com.loopers.domain.user.User
import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class UserFacade(
    private val userService: UserService,
) {
    fun signUp(loginId: String, password: String, name: String, email: String, birthday: LocalDate): UserInfo {
        return userService.signUp(loginId, password, name, email, birthday)
            .let { UserInfo.from(it) }
    }

    fun getMe(user: User): UserInfo {
        return UserInfo.fromWithMasking(user)
    }
}
