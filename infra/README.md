# KongLog 인프라

프론트엔드와 백엔드를 띄우는 실행 환경입니다. 로컬 개발용 DB와, 서버를 만들며 한 일의 기록이 있습니다.

## 폴더

| 폴더 | 내용 |
| --- | --- |
| `local/` | 로컬 개발용 DB. 실행 방법은 루트 [README.md](../README.md)의 "로컬에서 실행"에 있습니다 |
| `docker/` | Docker 이미지를 만드는 파일 |
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

## 서버를 처음부터 다시 만들 때

`setup/`의 문서를 번호 순서대로 따라 합니다. 콘솔 화면에서 손으로 한 일도 여기에 적습니다.

1. [AWS 계정과 도메인](setup/01-aws-account-and-domain.md)

## 비밀 값

이 저장소는 누구나 볼 수 있습니다. 비밀번호, 토큰, 접속 키, 서버 주소는 문서에 적지 않고, 어디에 넣는지만 적습니다.
