# KongLog 인프라

프론트엔드와 백엔드를 띄우는 실행 환경입니다. 로컬 개발용 DB와, 서버를 만들며 한 일의 기록이 있습니다.

## 폴더

| 폴더 | 내용 |
| --- | --- |
| `local/` | 로컬 개발용 DB. 실행 방법은 루트 [README.md](../README.md)의 "로컬에서 실행"에 있습니다 |
| `docker/` | Docker 이미지를 만드는 파일 |
| `server/` | 서버용 구성. Caddy, Next.js, Spring Boot, PostgreSQL을 함께 띄웁니다 |
| `setup/` | 서버를 처음 만들 때 한 일을 순서대로 적은 문서 |

## Docker 이미지

이미지는 프로그램과 실행에 필요한 것을 한데 묶은 것입니다. 서버에는 Java를 따로 깔지 않고 이 이미지를 받아 띄웁니다.

명령은 모두 저장소 루트에서 실행합니다.

### 백엔드

만들기:

```bash
docker build -f infra/docker/backend.Dockerfile -t konglog-backend:local backend
```

- 마지막의 `backend`는 빌드 대상 폴더입니다.
- 테스트는 돌리지 않습니다. 테스트는 DB가 필요해서 자동 검사가 맡습니다.
- 최종 이미지에는 실행 파일과 Java 실행 환경만 들어갑니다. 소스 코드와 Gradle은 들어가지 않습니다.

띄우기(로컬 DB가 떠 있어야 합니다):

```bash
docker run --rm -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/konglog \
  -e DB_USERNAME=konglog \
  -e DB_PASSWORD=konglog \
  konglog-backend:local
```

http://localhost:8080/api/ping 이 답하면 정상입니다.

- `host.docker.internal`은 컨테이너 안에서 내 컴퓨터를 가리키는 주소입니다. 컨테이너 안의 `localhost`는 컨테이너 자신이라서 로컬 DB에 닿지 않습니다.
- 위의 계정과 비밀번호는 로컬 전용입니다. 서버에서는 쓰지 않습니다.
- 8080 포트를 이미 쓰고 있으면 `-p 18080:8080`처럼 앞의 숫자를 바꿉니다.

설정은 띄울 때 환경 변수로 넣습니다.

| 환경 변수 | 설명 |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | DB 주소와 계정. 뜻은 [backend/README.md](../backend/README.md)의 "설정값"에 있습니다 |
| `JAVA_TOOL_OPTIONS` | Java에 넘길 옵션. 메모리 상한을 여기에 넣습니다. 예: `-e JAVA_TOOL_OPTIONS="-Xmx256m"` |

`-Xmx`는 Java가 객체를 담는 공간(힙)의 상한입니다. 컨테이너가 쓰는 전체 메모리는 이보다 큽니다. 서버에서 쓸 값은 서버용 Compose를 만들 때 정합니다.

### 프론트엔드

만들기:

```bash
docker build -f infra/docker/frontend.Dockerfile -t konglog-frontend:local frontend
```

- 백엔드가 떠 있지 않아도 만들어집니다. 글 데이터는 이미지를 만들 때가 아니라 화면을 요청받을 때 가져옵니다.
- 최종 이미지에는 빌드 결과와 실행에 필요한 패키지만 들어갑니다. 소스 코드와 `.env` 파일은 들어가지 않습니다.

띄우기(백엔드가 8080 포트에 떠 있어야 글이 보입니다):

```bash
docker run --rm -p 3000:3000 \
  -e API_BASE_URL=http://host.docker.internal:8080 \
  konglog-frontend:local
```

http://localhost:3000 에서 홈에 최근 글과 태그가 보이면 정상입니다.

설정은 띄울 때 환경 변수로 넣습니다.

| 환경 변수 | 설명 |
| --- | --- |
| `API_BASE_URL` | Next.js 서버가 백엔드를 부를 때 쓰는 주소. 이미지에 들어 있지 않아서 띄울 때 꼭 넣습니다 |
| `NODE_OPTIONS` | Node.js에 넘길 옵션. 메모리 상한을 넣을 때 씁니다. 예: `-e NODE_OPTIONS="--max-old-space-size=256"` |

## 서버용 구성

`server/compose.yaml`은 서버에서 쓰는 구성입니다. 네 가지를 명령 하나로 함께 띄웁니다.

| 이름 | 하는 일 |
| --- | --- |
| `caddy` | 밖에서 들어오는 요청을 받아 나눠 줍니다. 주소가 `/api`로 시작하면 백엔드로, 나머지는 프론트엔드로 보냅니다. HTTPS 인증서도 받습니다 |
| `frontend` | 화면 (Next.js) |
| `backend` | API 서버 (Spring Boot) |
| `db` | PostgreSQL 18 |

- 밖에 여는 포트는 Caddy의 80과 443뿐입니다. DB와 앱은 컨테이너끼리만 통합니다.
- DB 데이터와 인증서는 볼륨(컨테이너 밖의 저장 공간)에 둡니다. 컨테이너를 다시 만들어도 남습니다.
- 테이블은 백엔드가 뜰 때 Flyway가 만듭니다. `local/seed.sql`은 로컬 전용이라 여기서는 넣지 않습니다.

### 내 컴퓨터에서 띄워 보기

서버 없이 같은 구성을 확인할 때 씁니다. 로컬 개발용 DB(`local/compose.yaml`)가 떠 있어도 함께 띄울 수 있습니다.

1. 위의 "Docker 이미지"대로 백엔드와 프론트엔드 이미지를 만듭니다.
2. 환경 변수 파일을 만들고 `POSTGRES_USER`와 `POSTGRES_PASSWORD`를 채웁니다.
   ```bash
   cp infra/server/.env.example infra/server/.env
   ```
3. 띄웁니다.
   ```bash
   docker compose -f infra/server/compose.yaml up -d
   ```
4. 확인합니다. 내 컴퓨터에서는 Caddy가 직접 만든 인증서를 써서 `-k`(인증서 검사 건너뛰기)를 붙입니다.
   ```bash
   curl -k https://localhost/api/ping
   ```
   브라우저에서는 https://localhost 를 엽니다. 인증서 경고가 나오면 넘어갑니다. 처음에는 글이 없습니다.

내릴 때는 `docker compose -f infra/server/compose.yaml down`을 씁니다. 끝에 `-v`를 붙이면 DB 데이터와 인증서까지 지워집니다.

### 설정값

`server/.env`에 넣습니다. 이 파일은 저장소에 올리지 않습니다. 이름만 적힌 예시는 `server/.env.example`에 있습니다.

| 환경 변수 | 설명 |
| --- | --- |
| `SITE_DOMAIN` | 블로그 주소. 내 컴퓨터에서는 `localhost`, 서버에서는 `konglog.my` |
| `BLOCK_SEARCH_ENGINES` | 검색엔진 수집을 막을지. 없거나 `true`면 막고, `false`일 때만 풉니다. 아래 "검색엔진 수집 차단"에 있습니다 |
| `FRONTEND_IMAGE`, `BACKEND_IMAGE` | 띄울 이미지 이름 |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | DB 이름과 계정. DB를 처음 띄울 때 이 값으로 만들어집니다. 로컬 개발용 값(`konglog`)을 서버에서 쓰지 않습니다 |

`CORS_ALLOWED_ORIGINS`는 넣지 않습니다. 화면과 API가 같은 도메인이라 필요 없습니다.

### 검색엔진 수집 차단

블로그를 공개하기 전(3단계 전)에는 검색엔진이 수집하지 못하게 막아 둡니다. 서버를 띄우면 주소가 바로 열리는데, 그때는 글이 없고 만드는 중인 화면이라서입니다.

Caddy가 모든 응답에 아래 헤더(응답에 붙는 부가 정보)를 붙입니다. 검색엔진은 이 표시가 있는 주소를 검색 결과에 넣지 않습니다.

```
X-Robots-Tag: noindex, nofollow
```

- `noindex`는 "검색 결과에 넣지 말라", `nofollow`는 "이 화면의 링크를 따라가지 말라"는 뜻입니다.
- 화면과 `/api`에 모두 붙습니다. Caddy가 직접 만드는 오류 응답(앱이 죽었을 때의 502)에도 붙습니다.
- `http://`를 `https://`로 넘기는 응답에는 붙지 않습니다. 넘어간 뒤의 `https://` 응답에 붙어 있어서 그것으로 충분합니다.
- 사람이 여는 것은 막지 않습니다. 주소를 아는 사람은 그대로 볼 수 있습니다.
- `robots.txt`와 사이트맵은 3단계에서 만듭니다. 여기에는 없습니다.

확인:

```bash
curl -skI https://localhost/ | grep -i x-robots-tag
curl -skI https://localhost/api/ping | grep -i x-robots-tag
```

두 명령 모두 `x-robots-tag: noindex, nofollow`가 나오면 막힌 것입니다. 서버에서는 `localhost` 대신 `konglog.my`를 넣고 `-k`를 뺍니다.

#### 푸는 방법

블로그를 공개할 때 한 번 합니다.

1. `server/.env`에서 값을 바꿉니다.
   ```
   BLOCK_SEARCH_ENGINES=false
   ```
2. Caddy를 다시 만듭니다. 환경 변수는 컨테이너를 만들 때 들어가므로 `restart`로는 반영되지 않습니다. Caddy만 1초쯤 끊기고(내 컴퓨터에서 0.5~1.2초) 나머지 셋은 그대로 떠 있습니다.
   ```bash
   docker compose -f infra/server/compose.yaml up -d caddy
   ```
3. 위의 확인 명령을 다시 돌려 아무것도 나오지 않는지 봅니다.

다시 막을 때는 값을 `true`로 되돌리고 2번을 반복합니다.

- 값이 정확히 `false`일 때만 풀립니다. 줄이 없거나, 비었거나, `False`처럼 다르게 적혀 있으면 막힌 채로 있습니다. 실수로 풀리는 것보다 실수로 막혀 있는 쪽이 안전해서입니다.
- 풀어도 검색에 바로 나오지는 않습니다. 검색엔진이 다시 수집하러 올 때까지 시간이 걸립니다.

### 메모리 상한

2GB 서버에 네 가지를 올리므로 컨테이너마다 쓸 수 있는 메모리에 상한(`mem_limit`)을 두었습니다. 상한을 넘은 컨테이너는 멈췄다가 다시 뜹니다. 하나가 메모리를 다 써서 서버 전체가 멈추는 일을 막습니다.

| 이름 | 상한 | 안쪽 설정 | 요청을 보낸 뒤 잰 값 |
| --- | --- | --- | --- |
| `backend` | 640MB | 힙 256MB, 메타스페이스 128MB, 코드 캐시 64MB | 358MiB |
| `frontend` | 384MB | Node.js 힙 256MB | 141MiB |
| `db` | 256MB | `shared_buffers` 96MB, 접속 30개까지 | 45MiB |
| `caddy` | 128MB | 없음 | 29MiB |
| 합 | 1,408MB | | 573MiB |

정한 이유입니다.

- 서버의 메모리는 1.9GB이고, 아무것도 띄우지 않았을 때 운영체제가 약 380MB를 씁니다. 상한의 합(1,408MB)을 더해도 1.9GB를 넘지 않습니다.
- Java는 힙(객체를 담는 공간)만 막으면 전체 사용량이 그보다 커집니다. 힙 상한을 256MB로만 줬을 때 컨테이너는 410MiB를 썼습니다. 그래서 힙 밖의 큰 두 곳(메타스페이스, 코드 캐시)에도 상한을 두고, 컨테이너 상한은 세 값의 합(448MB)에 스레드 등이 쓸 여유를 더해 640MB로 잡았습니다.
- PostgreSQL의 `shared_buffers`(데이터를 메모리에 올려 두는 공간)는 기본값 128MB에서 96MB로 낮췄습니다. 글이 수백 개인 블로그에는 충분합니다.

잰 값은 내 컴퓨터(CPU 10개)에서 테스트용 글 24개를 넣고 잰 것입니다. 서버(CPU 2개)에서는 다시 잽니다.

## 서버를 처음부터 다시 만들 때

`setup/`의 문서를 번호 순서대로 따라 합니다. 콘솔 화면에서 손으로 한 일도 여기에 적습니다.

1. [AWS 계정과 도메인](setup/01-aws-account-and-domain.md)

## 비밀 값

이 저장소는 누구나 볼 수 있습니다. 비밀번호, 토큰, 접속 키, 서버 주소는 문서에 적지 않고, 어디에 넣는지만 적습니다.
