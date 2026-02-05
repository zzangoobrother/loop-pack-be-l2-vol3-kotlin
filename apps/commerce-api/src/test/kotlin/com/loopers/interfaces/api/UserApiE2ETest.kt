package com.loopers.interfaces.api

import com.loopers.interfaces.api.user.UserDto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val SIGNUP_ENDPOINT = "/api/v1/users/signup"
        private const val ME_ENDPOINT = "/api/v1/users/me"
        private const val CHANGE_PASSWORD_ENDPOINT = "/api/v1/users/me/password"
        private const val LOGIN_ID_HEADER = "X-Loopers-LoginId"
        private const val LOGIN_PW_HEADER = "X-Loopers-LoginPw"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun signUp(
        loginId: String,
        password: String,
        name: String,
        email: String,
        birthday: LocalDate,
    ) {
        val request = UserDto.SignUpRequest(
            loginId = loginId,
            password = password,
            name = name,
            email = email,
            birthday = birthday,
        )
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        testRestTemplate.exchange(
            SIGNUP_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(request, headers),
            object : ParameterizedTypeReference<ApiResponse<UserDto.SignUpResponse>>() {},
        )
    }

    @DisplayName("POST /api/v1/users/signup")
    @Nested
    inner class SignUp {
        @DisplayName("유효한 회원 정보로 가입하면, 회원이 생성되고 200 OK 응답을 받는다.")
        @Test
        fun returnsOk_whenValidUserInfoIsProvided() {
            // arrange
            val request = UserDto.SignUpRequest(
                loginId = "testuser123",
                password = "Test1234!@",
                name = "홍길동",
                email = "test@example.com",
                birthday = LocalDate.of(1990, 1, 15),
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserDto.SignUpResponse>>() {}
            val response = testRestTemplate.exchange(SIGNUP_ENDPOINT, HttpMethod.POST, httpEntity, responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.loginId).isEqualTo(request.loginId) },
                { assertThat(response.body?.data?.name).isEqualTo(request.name) },
                { assertThat(response.body?.data?.email).isEqualTo(request.email) },
            )
        }

        @DisplayName("로그인 ID가 빈 문자열이면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenLoginIdIsBlank() {
            // arrange
            val request = mapOf(
                "loginId" to "",
                "password" to "Test1234!@",
                "name" to "홍길동",
                "email" to "test@example.com",
                "birthday" to "1990-01-15",
            )
            val httpEntity = HttpEntity(request, jsonHeaders())

            // act
            val response = testRestTemplate.exchange(
                SIGNUP_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.message).contains("로그인 ID") },
            )
        }

        @DisplayName("로그인 ID에 특수문자가 포함되면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenLoginIdContainsSpecialCharacters() {
            // arrange
            val request = mapOf(
                "loginId" to "test@user!",
                "password" to "Test1234!@",
                "name" to "홍길동",
                "email" to "test@example.com",
                "birthday" to "1990-01-15",
            )
            val httpEntity = HttpEntity(request, jsonHeaders())

            // act
            val response = testRestTemplate.exchange(
                SIGNUP_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.message).contains("영문과 숫자만") },
            )
        }

        @DisplayName("비밀번호가 빈 문자열이면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenPasswordIsBlank() {
            // arrange
            val request = mapOf(
                "loginId" to "testuser123",
                "password" to "",
                "name" to "홍길동",
                "email" to "test@example.com",
                "birthday" to "1990-01-15",
            )
            val httpEntity = HttpEntity(request, jsonHeaders())

            // act
            val response = testRestTemplate.exchange(
                SIGNUP_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.message).contains("비밀번호") },
            )
        }

        @DisplayName("이름이 빈 문자열이면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNameIsBlank() {
            // arrange
            val request = mapOf(
                "loginId" to "testuser123",
                "password" to "Test1234!@",
                "name" to "",
                "email" to "test@example.com",
                "birthday" to "1990-01-15",
            )
            val httpEntity = HttpEntity(request, jsonHeaders())

            // act
            val response = testRestTemplate.exchange(
                SIGNUP_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.message).contains("이름") },
            )
        }

        @DisplayName("이메일 형식이 올바르지 않으면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenEmailIsInvalid() {
            // arrange
            val request = mapOf(
                "loginId" to "testuser123",
                "password" to "Test1234!@",
                "name" to "홍길동",
                "email" to "invalid-email",
                "birthday" to "1990-01-15",
            )
            val httpEntity = HttpEntity(request, jsonHeaders())

            // act
            val response = testRestTemplate.exchange(
                SIGNUP_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.message).contains("이메일") },
            )
        }

        private fun jsonHeaders(): HttpHeaders = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
    }

    @DisplayName("GET /api/v1/users/me")
    @Nested
    inner class GetMe {
        @DisplayName("유효한 인증 헤더로 요청하면, 200 OK와 마스킹된 이름을 반환한다.")
        @Test
        fun returnsOkWithMaskedName_whenValidCredentialsProvided() {
            // arrange - 먼저 회원가입
            val loginId = "testuser123"
            val password = "Test1234!@"
            val name = "홍길동"
            signUp(loginId, password, name, "test@example.com", LocalDate.of(1990, 1, 15))

            val headers = HttpHeaders().apply {
                set(LOGIN_ID_HEADER, loginId)
                set(LOGIN_PW_HEADER, password)
            }
            val httpEntity = HttpEntity<Void>(headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserDto.MeResponse>>() {}
            val response = testRestTemplate.exchange(ME_ENDPOINT, HttpMethod.GET, httpEntity, responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.loginId).isEqualTo(loginId) },
                { assertThat(response.body?.data?.name).isEqualTo("홍길*") },
                { assertThat(response.body?.data?.email).isEqualTo("test@example.com") },
                { assertThat(response.body?.data?.birthday).isEqualTo(LocalDate.of(1990, 1, 15)) },
            )
        }

        @DisplayName("인증 헤더가 누락되면, 401 Unauthorized 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenHeadersMissing() {
            // arrange
            val httpEntity = HttpEntity<Void>(HttpHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(ME_ENDPOINT, HttpMethod.GET, httpEntity, responseType)

            // assert
            assertThat(response.statusCode.value()).isEqualTo(401)
        }

        @DisplayName("존재하지 않는 로그인 ID로 요청하면, 401 Unauthorized 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLoginIdNotFound() {
            // arrange
            val headers = HttpHeaders().apply {
                set(LOGIN_ID_HEADER, "nonexistent")
                set(LOGIN_PW_HEADER, "Test1234!@")
            }
            val httpEntity = HttpEntity<Void>(headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(ME_ENDPOINT, HttpMethod.GET, httpEntity, responseType)

            // assert
            assertThat(response.statusCode.value()).isEqualTo(401)
        }

        @DisplayName("비밀번호가 일치하지 않으면, 401 Unauthorized 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenPasswordNotMatches() {
            // arrange - 먼저 회원가입
            val loginId = "testuser123"
            signUp(loginId, "Test1234!@", "홍길동", "test@example.com", LocalDate.of(1990, 1, 15))

            val headers = HttpHeaders().apply {
                set(LOGIN_ID_HEADER, loginId)
                set(LOGIN_PW_HEADER, "WrongPassword1!")
            }
            val httpEntity = HttpEntity<Void>(headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(ME_ENDPOINT, HttpMethod.GET, httpEntity, responseType)

            // assert
            assertThat(response.statusCode.value()).isEqualTo(401)
        }
    }

    @DisplayName("PATCH /api/v1/users/me/password")
    @Nested
    inner class ChangePassword {
        @DisplayName("유효한 현재 비밀번호와 새 비밀번호로 요청하면, 200 OK 응답을 받는다.")
        @Test
        fun returnsOk_whenValidPasswordsProvided() {
            // arrange - 먼저 회원가입
            val loginId = "testuser123"
            val currentPassword = "Test1234!@"
            val newPassword = "NewPass1234!@"
            signUp(loginId, currentPassword, "홍길동", "test@example.com", LocalDate.of(1990, 1, 15))

            val request = UserDto.ChangePasswordRequest(
                currentPassword = currentPassword,
                newPassword = newPassword,
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LOGIN_ID_HEADER, loginId)
                set(LOGIN_PW_HEADER, currentPassword)
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                CHANGE_PASSWORD_ENDPOINT,
                HttpMethod.PATCH,
                httpEntity,
                responseType,
            )

            // assert
            assertThat(response.statusCode.is2xxSuccessful).isTrue()
        }

        @DisplayName("현재 비밀번호가 일치하지 않으면, 401 Unauthorized 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenCurrentPasswordNotMatches() {
            // arrange
            val loginId = "testuser123"
            val currentPassword = "Test1234!@"
            signUp(loginId, currentPassword, "홍길동", "test@example.com", LocalDate.of(1990, 1, 15))

            val request = UserDto.ChangePasswordRequest(
                currentPassword = "WrongPassword1!",
                newPassword = "NewPass1234!@",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LOGIN_ID_HEADER, loginId)
                set(LOGIN_PW_HEADER, currentPassword)
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                CHANGE_PASSWORD_ENDPOINT,
                HttpMethod.PATCH,
                httpEntity,
                responseType,
            )

            // assert
            assertThat(response.statusCode.value()).isEqualTo(401)
        }

        @DisplayName("새 비밀번호 형식이 올바르지 않으면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNewPasswordFormatInvalid() {
            // arrange
            val loginId = "testuser123"
            val currentPassword = "Test1234!@"
            signUp(loginId, currentPassword, "홍길동", "test@example.com", LocalDate.of(1990, 1, 15))

            val request = UserDto.ChangePasswordRequest(
                currentPassword = currentPassword,
                newPassword = "weak",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LOGIN_ID_HEADER, loginId)
                set(LOGIN_PW_HEADER, currentPassword)
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                CHANGE_PASSWORD_ENDPOINT,
                HttpMethod.PATCH,
                httpEntity,
                responseType,
            )

            // assert
            assertThat(response.statusCode.value()).isEqualTo(400)
        }

        @DisplayName("새 비밀번호에 생년월일이 포함되면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNewPasswordContainsBirthday() {
            // arrange
            val loginId = "testuser123"
            val currentPassword = "Test1234!@"
            val birthday = LocalDate.of(1990, 1, 15)
            signUp(loginId, currentPassword, "홍길동", "test@example.com", birthday)

            val request = UserDto.ChangePasswordRequest(
                currentPassword = currentPassword,
                newPassword = "Pass19900115!@",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LOGIN_ID_HEADER, loginId)
                set(LOGIN_PW_HEADER, currentPassword)
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                CHANGE_PASSWORD_ENDPOINT,
                HttpMethod.PATCH,
                httpEntity,
                responseType,
            )

            // assert
            assertThat(response.statusCode.value()).isEqualTo(400)
        }

        @DisplayName("현재 비밀번호와 새 비밀번호가 동일하면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNewPasswordSameAsCurrent() {
            // arrange
            val loginId = "testuser123"
            val currentPassword = "Test1234!@"
            signUp(loginId, currentPassword, "홍길동", "test@example.com", LocalDate.of(1990, 1, 15))

            val request = UserDto.ChangePasswordRequest(
                currentPassword = currentPassword,
                newPassword = currentPassword,
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LOGIN_ID_HEADER, loginId)
                set(LOGIN_PW_HEADER, currentPassword)
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                CHANGE_PASSWORD_ENDPOINT,
                HttpMethod.PATCH,
                httpEntity,
                responseType,
            )

            // assert
            assertThat(response.statusCode.value()).isEqualTo(400)
        }

        @DisplayName("인증 헤더가 누락되면, 401 Unauthorized 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenAuthHeadersMissing() {
            // arrange
            val request = UserDto.ChangePasswordRequest(
                currentPassword = "Test1234!@",
                newPassword = "NewPass1234!@",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                CHANGE_PASSWORD_ENDPOINT,
                HttpMethod.PATCH,
                httpEntity,
                responseType,
            )

            // assert
            assertThat(response.statusCode.value()).isEqualTo(401)
        }
    }
}
