package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    fun signUp(loginId: String, password: String, name: String, email: String, birthday: LocalDate): User {
        if (userRepository.existsByLoginId(loginId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID 입니다.")
        }

        val validatedPassword = Password.of(password, birthday)
        val encodedPassword = passwordEncoder.encode(validatedPassword.value)

        val user = User(
            loginId = loginId,
            password = encodedPassword,
            name = name,
            email = email,
            birthday = birthday,
        )
        return userRepository.save(user)
    }

    fun authenticate(loginId: String, password: String): User {
        val user = userRepository.findByLoginId(loginId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "존재하지 않는 사용자입니다.")

        if (!passwordEncoder.matches(password, user.password)) {
            throw CoreException(ErrorType.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.")
        }

        return user
    }
}
