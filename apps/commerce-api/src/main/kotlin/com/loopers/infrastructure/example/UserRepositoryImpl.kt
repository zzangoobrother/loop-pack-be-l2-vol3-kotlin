package com.loopers.infrastructure.example

import com.loopers.domain.example.User
import com.loopers.domain.example.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {
    override fun find(id: Long): User? {
        return userJpaRepository.findByIdOrNull(id)
    }

    override fun existsByLoginId(loginId: String): Boolean {
        return userJpaRepository.existsByLoginId(loginId)
    }

    override fun save(user: User): User {
        return userJpaRepository.save(user)
    }
}
