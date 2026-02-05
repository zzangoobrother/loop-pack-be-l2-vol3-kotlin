package com.loopers.support.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.user.User
import com.loopers.domain.user.UserService
import com.loopers.support.error.ErrorType
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class AuthenticationFilterTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var filterChain: FilterChain

    private val objectMapper = ObjectMapper()

    private lateinit var authenticationFilter: AuthenticationFilter

    @BeforeEach
    fun setUp() {
        authenticationFilter = AuthenticationFilter(userService, objectMapper)
    }

    @DisplayName("인증 필터가")
    @Nested
    inner class DoFilter {
        @DisplayName("인증이 필요한 경로에서 유효한 헤더가 있으면, 인증된 사용자를 request에 저장한다.")
        @Test
        fun setsAuthenticatedUser_whenValidHeadersProvided() {
            // arrange
            val request = MockHttpServletRequest("GET", "/api/v1/users/me")
            val response = MockHttpServletResponse()
            val loginId = "testuser"
            val password = "Test1234!@"
            val user = User(
                loginId = loginId,
                password = "encoded",
                name = "홍길동",
                email = "test@example.com",
                birthday = LocalDate.of(1990, 1, 1),
            )

            request.addHeader("X-Loopers-LoginId", loginId)
            request.addHeader("X-Loopers-LoginPw", password)

            whenever(userService.authenticate(loginId, password)).thenReturn(user)

            // act
            authenticationFilter.doFilter(request, response, filterChain)

            // assert
            val authenticatedUser = request.getAttribute("authenticatedUser") as User
            assertThat(authenticatedUser.loginId).isEqualTo(loginId)
            verify(filterChain).doFilter(request, response)
        }

        @DisplayName("인증이 필요 없는 경로에서는 인증을 수행하지 않는다.")
        @Test
        fun skipsAuthentication_whenPathDoesNotRequireAuth() {
            // arrange
            val request = MockHttpServletRequest("POST", "/api/v1/users/signup")
            val response = MockHttpServletResponse()

            // act
            authenticationFilter.doFilter(request, response, filterChain)

            // assert
            verify(userService, never()).authenticate(any(), any())
            verify(filterChain).doFilter(request, response)
        }

        @DisplayName("인증이 필요한 경로에서 헤더가 없으면, 401 응답을 반환한다.")
        @Test
        fun returnsUnauthorized_whenHeadersMissing() {
            // arrange
            val request = MockHttpServletRequest("GET", "/api/v1/users/me")
            val response = MockHttpServletResponse()

            // act
            authenticationFilter.doFilter(request, response, filterChain)

            // assert
            assertThat(response.status).isEqualTo(ErrorType.UNAUTHORIZED.status.value())
            verify(filterChain, never()).doFilter(any(), any())
        }
    }
}
