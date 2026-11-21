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

테스트는 로컬 DB 안의 `konglog_test` 스키마(테이블을 담는 별도 공간)만 씁니다. 개발하며 넣어 둔 글과 섞이지 않고, 테스트가 끝나면 넣은 데이터를 되돌립니다. 설정은 `src/test/resources/application-test.yaml`에 있습니다.

## API

주소는 모두 `/api`로 시작합니다. 독자에게는 상태가 "게시"인 글만 내려줍니다.

### `GET /api/posts` 글 목록

게시 글을 발행일 최신순으로 내려줍니다.

| 값 | 설명 |
| --- | --- |
| `size` | 받을 개수. 1부터 20까지, 기본 20 |
| `tag` | 태그 이름. 이 태그가 붙은 글만 내려줍니다. 없거나 비어 있으면 거르지 않습니다 |
| `cursor` | 앞선 응답의 `nextCursor`. 그다음 글부터 내려줍니다. 없으면 처음부터입니다 |

```json
{
  "posts": [
    {
      "title": "Docker 컨테이너에서 localhost가 안 잡혔던 이유",
      "slug": "docker-localhost",
      "summary": "컨테이너 안에서 localhost는 컨테이너 자신이었어요.",
      "tags": ["Docker", "네트워크"],
      "publishedAt": "2026-11-18T00:00:00Z"
    }
  ],
  "totalCount": 23,
  "nextCursor": "MjAyNi0xMS0wNFQwMDowMDowMFosMw"
}
```

- `tags`는 태그 이름이고 이름순입니다. 이 이름을 그대로 `tag`에 넣어 거릅니다.
- `publishedAt`은 시간대가 들어간 ISO 8601 형식이고 항상 UTC(`Z`)입니다.
- `totalCount`는 조건(`tag`)에 맞는 게시 글의 전체 수입니다. 이번에 내려준 개수가 아닙니다.
- `nextCursor`는 더 받을 글이 없으면 `null`입니다. 뜻을 풀지 말고 받은 그대로 `cursor`에 넣습니다. 이어 받을 때는 `tag`와 `size`를 처음과 같게 넣습니다.
- 조건에 맞는 글이 없으면 오류가 아니라 빈 목록(`posts: []`, `totalCount: 0`)입니다. 없는 태그로 걸러도 같습니다.
- `size`가 범위를 벗어나거나 `cursor`가 이 API가 준 값이 아니면 400입니다.

### `GET /api/posts/{슬러그}` 글 상세

게시 글 하나를 내려줍니다.

```json
{
  "title": "Docker 컨테이너에서 localhost가 안 잡혔던 이유",
  "slug": "docker-localhost",
  "summary": "컨테이너 안에서 localhost는 컨테이너 자신이었어요.",
  "content": "## 문제\n\n컨테이너 안에서 `localhost`가 안 잡혔어요.",
  "tags": ["Docker", "네트워크"],
  "publishedAt": "2026-11-18T00:00:00Z",
  "editedAt": null
}
```

- `content`는 마크다운 원문 그대로입니다.
- `editedAt`은 수정일입니다. 게시한 뒤 본문을 고친 적이 없으면 `null`이고, 있으면 `publishedAt`과 같은 형식입니다.
- 없는 글이거나 초안이면 404입니다. 초안이 있다는 사실이 드러나지 않도록 두 경우의 응답이 같습니다.

### `GET /api/tags` 태그 목록

게시 글이 있는 태그만, 글이 많은 순으로 내려줍니다. 글 수가 같으면 이름순입니다.

```json
{
  "tags": [
    { "name": "Spring", "postCount": 6 },
    { "name": "React", "postCount": 5 }
  ]
}
```

- `name`을 그대로 글 목록의 `tag`에 넣어 거릅니다. 태그를 가리키는 값은 이름 하나뿐입니다.
- `postCount`에는 게시 글만 셉니다.
- 게시 글이 하나도 없으면 빈 목록(`tags: []`)입니다.

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
