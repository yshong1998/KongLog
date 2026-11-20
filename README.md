# KongLog

개발하다 만난 문제를 콩알만큼씩 적어 두는 개인 기술 블로그입니다.

## 구조

| 폴더 | 내용 |
| --- | --- |
| `frontend/` | 화면. Next.js 16, TypeScript, Tailwind CSS |
| `backend/` | API 서버. Spring Boot 4.1, Java 25, PostgreSQL |
| `infra/` | 실행 환경 설정. 지금은 로컬 개발용 DB만 있습니다 |
| `docs/` | 작업하며 배운 점 기록. 나중에 블로그 글이 됩니다 |
| `.github/` | 자동 검사, 이슈 템플릿, 풀 리퀘스트 템플릿 |

## 준비물

- Node.js 22 이상
- JDK 25
- Docker

## 로컬에서 실행

터미널 세 개에서 순서대로 실행합니다.

```bash
# 1. DB
docker compose -f infra/local/compose.yaml up -d

# 2. 백엔드 (http://localhost:8080)
cd backend
./gradlew bootRun

# 3. 프론트엔드 (http://localhost:3000)
cd frontend
npm install
cp .env.example .env.local
npm run dev
```

http://localhost:3000 을 열었을 때 "백엔드 연결" 칸에 "연결됐어요"가 보이면 세 가지가 모두 이어진 것입니다.

DB를 내릴 때는 `docker compose -f infra/local/compose.yaml down`을 씁니다. 데이터까지 지우려면 끝에 `-v`를 붙입니다.

## 검사와 테스트

```bash
# 프론트엔드
cd frontend
npm run lint && npm run typecheck && npm test && npm run build

# 백엔드 (로컬 DB가 떠 있어야 합니다)
cd backend
./gradlew test
```

같은 검사가 GitHub Actions에서도 자동으로 돕니다.

## 작업 방식

이슈를 먼저 만들고, 브랜치에서 작업한 뒤 풀 리퀘스트로 올립니다. 브랜치 이름과 커밋 메시지 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md)에 있습니다.

## 개발 단계

| 단계 | 내용 | 상태 |
| --- | --- | --- |
| 0 | 준비: 저장소, 프로젝트 뼈대, 자동 검사 | 진행 중 |
| 1 | 글 읽기: 홈, 글 목록, 글 상세 | |
| 2 | 배포: 서버, HTTPS, 자동 배포 | |
| 3 | 글 쓰기와 첫 공개: 에디터, 초안 API | |
| 4 | 회원: 가입, 로그인, 계정 통합 | |
| 5 | 참여: 댓글, 좋아요, 조회수 | |
| 6 | 다듬기: 검색, 다크 모드, RSS | |
