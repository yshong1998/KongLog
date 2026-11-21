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

## 테스트용 글 넣기

화면을 만들며 확인할 글을 로컬 DB에 넣습니다. 서버를 한 번 실행해 테이블이 만들어진 뒤에, 저장소 루트에서 실행합니다.

```bash
docker compose -f infra/local/compose.yaml exec -T db psql -U konglog -d konglog < infra/local/seed.sql
```

- 게시 글 24개와 초안 2개가 들어갑니다. 끝나면 상태별 글 수가 출력됩니다.
- 여러 번 실행해도 결과가 같습니다. 슬러그가 같은 글만 덮어쓰고, 직접 써 넣은 글은 건드리지 않습니다.
- 로컬 전용입니다. Flyway 파일이 아니라서 배포 환경의 DB에는 들어가지 않습니다.
- 발행일이 같은 글, 태그가 없는 글처럼 일부러 넣어 둔 경우는 `infra/local/seed.sql` 맨 위에 적혀 있습니다.

## 명령

| 명령 | 하는 일 |
| --- | --- |
| `./gradlew bootRun` | 서버 실행 |
| `./gradlew test` | 테스트. 로컬 DB가 떠 있어야 합니다 |
| `./gradlew build` | 테스트 후 실행 파일 만들기 |

테스트는 로컬 DB 안의 `konglog_test` 스키마(테이블을 담는 별도 공간)만 씁니다. 개발하며 넣어 둔 글과 섞이지 않고, 테스트가 끝나면 넣은 데이터를 되돌립니다. 설정은 `src/test/resources/application-test.yaml`에 있습니다.

## 구조

- `src/main/java/my/konglog`: 코드. 기능별로 폴더를 나눕니다 (`post`, `tag`, `ping`)
- `src/main/resources/application.yaml`: 설정
- `src/main/resources/db/migration`: 테이블 변경 파일. `V2__add_member.sql`처럼 번호를 올려 추가하고, 한번 적용된 파일은 고치지 않습니다

## 설정값

| 환경 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/konglog` | DB 주소 |
| `DB_USERNAME` | `konglog` | DB 사용자 |
| `DB_PASSWORD` | `konglog` | DB 비밀번호. 기본값은 로컬 전용입니다 |
