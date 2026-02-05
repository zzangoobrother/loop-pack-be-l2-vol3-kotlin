package com.loopers.support.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.user.UserService
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.stereotype.Component

@Component
@Order(1)
class AuthenticationFilter(
    private val userService: UserService,
    private val objectMapper: ObjectMapper,
) : Filter {

    companion object {
        private const val LOGIN_ID_HEADER = "X-Loopers-LoginId"
        private const val LOGIN_PW_HEADER = "X-Loopers-LoginPw"
        const val AUTHENTICATED_USER_ATTRIBUTE = "authenticatedUser"

        private val AUTH_EXCLUDE_PATHS = listOf(
            "/api/v1/users/signup",
            "/api/v1/examples",
            "/actuator",
            "/swagger",
            "/v3/api-docs",
        )
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        try {
            if (requiresAuthentication(httpRequest)) {
                val loginId = httpRequest.getHeader(LOGIN_ID_HEADER)
                val password = httpRequest.getHeader(LOGIN_PW_HEADER)

                if (loginId.isNullOrBlank() || password.isNullOrBlank()) {
                    writeErrorResponse(httpResponse, ErrorType.UNAUTHORIZED, "인증 헤더가 필요합니다.")
                    return
                }

                val user = userService.authenticate(loginId, password)
                httpRequest.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, user)
            }

            chain.doFilter(request, response)
        } catch (e: CoreException) {
            writeErrorResponse(httpResponse, e.errorType, e.message ?: e.errorType.message)
        }
    }

    private fun requiresAuthentication(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return AUTH_EXCLUDE_PATHS.none { path.startsWith(it) }
    }

    private fun writeErrorResponse(response: HttpServletResponse, errorType: ErrorType, message: String) {
        response.status = errorType.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"

        val errorResponse = ApiResponse.fail(errorType.code, message)
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
