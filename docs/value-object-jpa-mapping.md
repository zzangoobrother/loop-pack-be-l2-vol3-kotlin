# Value Object JPA 매핑 전략

## 개요

DDD에서 Value Object를 JPA Entity에 매핑하는 두 가지 방식을 비교 분석한다.

- **String 저장 방식**: Entity 필드는 String, Value Object는 검증용으로만 사용
- **@Embedded 방식**: Value Object를 JPA @Embeddable로 직접 매핑

## 현재 프로젝트 구조

### Value Object (검증 로직 캡슐화)

```kotlin
// LoginId.kt
class LoginId private constructor(val value: String) {
    companion object {
        fun of(value: String): LoginId {
            validate(value)
            return LoginId(value)
        }
    }
}
```

### Entity (String 저장 + 검증용 Value Object)

```kotlin
// User.kt
@Entity
class User(
    loginId: String,
    password: String,
    email: String,
    // ...
) : BaseEntity() {

    var loginId: String = loginId
        protected set

    init {
        LoginId.of(loginId)  // 검증만 수행
        Email.of(email)      // 검증만 수행
    }
}
```

## 방식 비교

### 1. String 저장 방식 (현재)

```kotlin
// Entity
var loginId: String = loginId
    protected set

init {
    LoginId.of(loginId)  // 검증만 수행, 결과는 버림
}
```

### 2. @Embedded 방식

```kotlin
// Value Object
@Embeddable
class LoginId private constructor(
    @Column(name = "login_id")
    val value: String
) {
    companion object {
        fun of(value: String): LoginId { /* 검증 */ }
    }
}

// Entity
@Embedded
var loginId: LoginId = LoginId.of(loginId)
    protected set
```

## 비교 분석

| 관점 | String (현재) | @Embedded |
|------|--------------|-----------|
| **타입 안정성** | ❌ Entity 내부에서 String | ✅ 타입으로 구분 가능 |
| **DDD 순수성** | △ 검증만 위임 | ✅ 완전한 Value Object |
| **JPA 복잡도** | ✅ 단순 | ❌ AttributeOverride, Converter 필요 |
| **DB 스키마** | ✅ 변경 없음 | ✅ 동일 (단일 컬럼) |
| **쿼리 작성** | ✅ `where loginId = ?` | △ `where loginId.value = ?` |
| **직렬화** | ✅ 자동 | △ 추가 설정 필요 |
| **성능** | ✅ 오버헤드 없음 | ✅ 거의 동일 |

## 권장 사항

### String 저장 방식을 권장하는 경우

1. **단일 값 Value Object**
   - LoginId, Email처럼 하나의 String 값만 감싸는 경우
   - 검증 로직만 캡슐화하면 충분한 경우

2. **암호화/변환이 필요한 필드**
   - Password처럼 저장 시 변환이 필요한 경우
   - Value Object와 저장 값이 1:1 매핑되지 않는 경우

3. **실용성 우선**
   - JPA 설정 복잡도를 줄이고 싶은 경우
   - Repository 쿼리를 단순하게 유지하고 싶은 경우

### @Embedded 방식을 권장하는 경우

1. **복합 값 Value Object**
   - Address(city, street, zipCode)처럼 여러 필드를 가지는 경우
   - 하나의 개념이 여러 컬럼에 매핑되는 경우

2. **도메인 메서드가 필요한 경우**
   - Money.add(other), DateRange.contains(date) 등
   - Value Object에 비즈니스 로직이 있는 경우

3. **타입 안정성이 중요한 경우**
   - 컴파일 타임에 타입 검증이 필요한 경우
   - 실수로 잘못된 값을 할당하는 것을 방지해야 하는 경우

## 프로젝트 적용 결론

| 필드 | 저장 방식 | 이유 |
|------|----------|------|
| `loginId` | String | 단일 값, 검증만 필요 |
| `email` | String | 단일 값, 검증만 필요 |
| `password` | String | 암호화된 값 저장, Value Object와 1:1 매핑 불가 |

현재 구조는 다음 DDD 목적을 달성한다:

- ✅ 도메인 규칙이 Value Object에 캡슐화됨
- ✅ Entity 생성 시 자동 검증됨
- ✅ Value Object가 다른 곳에서 재사용 가능
- ✅ JPA 매핑이 단순하고 명확함

## 참고

- [Vaughn Vernon - Implementing Domain-Driven Design](https://www.amazon.com/Implementing-Domain-Driven-Design-Vaughn-Vernon/dp/0321834577)
- [JPA @Embeddable vs AttributeConverter](https://thorben-janssen.com/jpa-attribute-converter/)
