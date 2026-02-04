package com.loopers.domain.example

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

        validatePassword(password, birthday)

        val encodedPassword = passwordEncoder.encode(password)
        val user = User(
            loginId = loginId,
            password = encodedPassword,
            name = name,
            email = email,
            birthday = birthday,
        )
        return userRepository.save(user)
    }

    private fun validatePassword(password: String, birthday: LocalDate) {
        if (!PASSWORD_PATTERN.matches(password)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 8~16자의 영문 대소문자, 숫자, 특수문자를 포함해야 합니다.")
        }
        val birthdayString = birthday.toString().replace("-", "")
        if (password.contains(birthdayString)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 생년월일을 포함할 수 없습니다.")
        }
    }

    companion object {
        private val PASSWORD_PATTERN = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&#]{8,16}$")
    }
}
