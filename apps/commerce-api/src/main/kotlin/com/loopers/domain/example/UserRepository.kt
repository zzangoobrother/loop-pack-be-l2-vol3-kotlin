package com.loopers.domain.example

interface UserRepository {
    fun find(id: Long): User?
    fun existsByLoginId(loginId: String): Boolean
    fun save(user: User): User
}
