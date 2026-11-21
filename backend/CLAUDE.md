# KongLog 백엔드 세션

이 폴더에서 실행된 Claude는 백엔드 담당이다. 공통 규칙(담당 범위, worktree, 세션끼리 주고받기)은 루트 `CLAUDE.md`를 따르고, 여기에는 이 세션에만 해당하는 것을 적는다.

## 작업 순서

1. 깃허브에서 열린 이슈를 확인한다. 영역이 `backend`인 이슈만 맡고, 본문의 할 일과 끝나는 기준을 읽는다.
2. `cowork/`에 아직 완료 표시가 없는 `front_req_*.md`가 있는지 확인한다.
3. 백엔드 작업을 진행한다.

## 한 번에 이슈 하나

- 한 번에 이슈 하나, 브랜치 하나만 구현한다. 열린 이슈가 여러 개여도 번호가 가장 앞선 것 하나만 한다.
- 풀 리퀘스트를 올리면 멈추고 사용자에게 알린다. 다음 이슈는 사용자가 시키면 시작한다.
- 앞선 풀 리퀘스트 위에 다음 브랜치를 쌓지 않는다.

## 프론트엔드와 주고받기

- 프론트에 요청할 것이 생기면 작업을 멈추고 `cowork/issue_<이슈번호>/back_req_<순번>.md`를 만든다. 필요한 것, 필요한 이유, 바라는 모양을 적는다.
- `front_req_*.md`의 요청을 끝내면 그 문서 맨 위에 구현이 완료됐다는 것과 실제 API 모양을 적는다.
- 요청한 것이 지금 맡은 이슈의 범위 밖이면 구현하지 않고, 어느 이슈에서 다룰지만 사용자에게 알린다.

## 코드 규칙

아래 경로는 이 폴더(`backend/`) 기준이다.

- 패키지는 `my.konglog` 아래 기능별로 나눈다 (`post`, `tag`, `member` 등).
- API 주소는 모두 `/api`로 시작한다.
- 테이블은 Flyway 파일로만 바꾼다. `src/main/resources/db/migration`에 번호를 올려 새 파일을 추가하고, 이미 적용된 파일은 고치지 않는다.
- 엔티티를 API 응답으로 그대로 내보내지 않는다. 응답용 `record`를 따로 둔다.
- Lombok은 `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@RequiredArgsConstructor`만 쓴다. 엔티티에 `@Setter`, `@Data`, `@Builder`, `@AllArgsConstructor`, `@EqualsAndHashCode`, `@ToString`을 붙이지 않는다. 엔티티는 정해 둔 메서드(`draft()`, `publish()` 등)로만 만들고 바꾼다.
- 새 API에는 테스트를 함께 쓴다. 테스트 이름은 한국어로 쓴다.
- 비밀값은 커밋하지 않는다. 환경 변수로 받는다.
- **Spring Boot 4는 3과 의존성 이름이 다르다** (`spring-boot-starter-webmvc`, `spring-boot-starter-flyway`, 기술별 `-test` 스타터 등). 추가하기 전에 4.1 공식 문서에서 이름을 확인한다.
