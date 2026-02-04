package com.loopers.domain.user

interface UserRepository {
    fun find(id: Long): User?
    fun findByLoginId(loginId: String): User?
    fun existsByLoginId(loginId: String): Boolean
    fun save(user: User): User
}
