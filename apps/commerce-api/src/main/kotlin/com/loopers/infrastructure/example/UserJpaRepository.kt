package com.loopers.infrastructure.example

import com.loopers.domain.example.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<User, Long> {
    fun existsByLoginId(loginId: String): Boolean
}
