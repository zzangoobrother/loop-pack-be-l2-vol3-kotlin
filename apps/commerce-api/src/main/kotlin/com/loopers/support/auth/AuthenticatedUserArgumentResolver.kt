package com.loopers.support.auth

import com.loopers.domain.user.User
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AuthenticatedUserArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthenticatedUser::class.java) &&
            parameter.parameterType == User::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): User {
        val user = webRequest.getAttribute(
            AuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE,
            RequestAttributes.SCOPE_REQUEST,
        ) as? User

        return user ?: throw CoreException(ErrorType.UNAUTHORIZED, "인증된 사용자 정보가 없습니다.")
    }
}
