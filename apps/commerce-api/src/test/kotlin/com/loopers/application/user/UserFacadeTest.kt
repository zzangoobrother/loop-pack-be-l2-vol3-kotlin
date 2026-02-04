package com.loopers.application.user

import com.loopers.domain.user.User
import com.loopers.domain.user.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class UserFacadeTest {

    @Mock
    private lateinit var userService: UserService

    private lateinit var userFacade: UserFacade

    @BeforeEach
    fun setUp() {
        userFacade = UserFacade(userService)
    }

    @DisplayName("내 정보 조회 할 때,")
    @Nested
    inner class GetMe {
        @DisplayName("인증된 사용자를 전달하면, 마스킹된 이름이 포함된 사용자 정보를 반환한다.")
        @Test
        fun returnsUserInfoWithMaskedName_whenAuthenticatedUserProvided() {
            // arrange
            val user = User(
                loginId = "testuser",
                password = "encoded_password",
                name = "홍길동",
                email = "test@example.com",
                birthday = LocalDate.of(1990, 1, 1),
            )

            // act
            val result = userFacade.getMe(user)

            // assert
            assertAll(
                { assertThat(result.loginId).isEqualTo("testuser") },
                { assertThat(result.maskedName).isEqualTo("홍길*") },
                { assertThat(result.email).isEqualTo("test@example.com") },
                { assertThat(result.birthday).isEqualTo(LocalDate.of(1990, 1, 1)) },
            )
        }
    }
}
