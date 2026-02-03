package com.loopers.domain.example

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "users")
class User(
    loginId: String,
    password: String,
    name: String,
    birthday: LocalDate,
    email: String,
) : BaseEntity() {

    var loginId: String = loginId
        protected set

    var password: String = password
        protected set

    var name: String = name
        protected set

    var birthday: LocalDate = birthday
        protected set

    var email: String = email
        protected set

    init {
        validateLoginId(loginId)
        validatePassword(password, birthday)
        validateName(name)
        validateEmail(email)
    }

    private fun validateLoginId(loginId: String) {
        if (loginId.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 비어있을 수 없습니다.")
        }
        if (!LOGIN_ID_PATTERN.matches(loginId)) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 영문과 숫자만 사용할 수 있습니다.")
        }
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

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.")
        }
    }

    private fun validateEmail(email: String) {
        if (!EMAIL_PATTERN.matches(email)) {
            throw CoreException(ErrorType.BAD_REQUEST, "올바른 이메일 형식이 아닙니다.")
        }
    }

    companion object {
        private val LOGIN_ID_PATTERN = Regex("^[a-zA-Z0-9]+$")
        private val PASSWORD_PATTERN = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&#]{8,16}$")
        private val EMAIL_PATTERN = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
    }
}
