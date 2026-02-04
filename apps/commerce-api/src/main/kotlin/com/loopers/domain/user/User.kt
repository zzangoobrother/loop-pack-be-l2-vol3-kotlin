package com.loopers.domain.user

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
        LoginId.of(loginId)
        validateName(name)
        Email.of(email)
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.")
        }
    }

    fun changePassword(newPassword: String) {
        if (this.password == newPassword) {
            throw CoreException(ErrorType.BAD_REQUEST, "새 비밀번호는 현재 비밀번호와 달라야 합니다.")
        }
        this.password = newPassword
    }
}
