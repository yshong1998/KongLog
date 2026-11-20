# KongLog 백엔드

Spring Boot 4.1, Java 25, Gradle로 만든 API 서버입니다. DB는 PostgreSQL이고 테이블 변경은 Flyway로 관리합니다.

## 준비물

- JDK 25
- Docker (로컬 DB용)

## 실행

저장소 루트에서 로컬 DB를 먼저 띄웁니다.

```bash
docker compose -f infra/local/compose.yaml up -d
```

그다음 이 폴더에서 서버를 실행합니다.

```bash
./gradlew bootRun
```

http://localhost:8080/api/ping 에서 `{"status":"ok","service":"konglog-backend"}`가 보이면 정상입니다.

## 명령

| 명령 | 하는 일 |
| --- | --- |
| `./gradlew bootRun` | 서버 실행 |
| `./gradlew test` | 테스트. 로컬 DB가 떠 있어야 합니다 |
| `./gradlew build` | 테스트 후 실행 파일 만들기 |

## 구조

- `src/main/java/my/konglog`: 코드. 기능별로 폴더를 나눕니다 (예: `ping`)
- `src/main/resources/application.yaml`: 설정
- `src/main/resources/db/migration`: 테이블 변경 파일. `V2__...sql`처럼 번호를 올려 추가하고, 한번 적용된 파일은 고치지 않습니다

## 설정값

| 환경 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/konglog` | DB 주소 |
| `DB_USERNAME` | `konglog` | DB 사용자 |
| `DB_PASSWORD` | `konglog` | DB 비밀번호. 기본값은 로컬 전용입니다 |
