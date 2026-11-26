---
title: "공개 전 블로그를 검색엔진에서 숨기려고 robots.txt 대신 응답 헤더를 골랐다"
date: 2026-11-26
stage: 2
type: 결정
tags: [Caddy, Docker Compose, SEO, X-Robots-Tag]
status: 메모
related: 이슈 #58, 풀 리퀘스트 #67, 커밋 687b7a3, d042bbf, infra/server/Caddyfile, infra/server/compose.yaml, infra/server/.env.example, infra/README.md
---

## 한 줄 요약

공개 전까지 Caddy가 모든 응답에 `X-Robots-Tag: noindex, nofollow`를 붙이게 했다. `.env`의 `BLOCK_SEARCH_ENGINES`가 정확히 `false`일 때만 풀리고, 값이 없거나 잘못 적혀 있으면 막힌 채로 있다.

## 상황

2단계에서 서버를 띄우면 konglog.my 주소가 바로 열린다. 그때는 글이 없고 만드는 중인 화면이다. 기획은 "3단계에서 공개하기 전까지 검색엔진 수집을 막아 둔다"까지만 정했고, 방법은 이슈 #58에서 고르기로 했다.

처음 든 의문: 검색에 나와야 사람들이 글을 보는데 왜 막나. 계속 막는 것이 아니라 공개 전까지만이다. 그 사이에 수집되면 이런 일이 생긴다.

- 검색엔진은 수집한 시점의 제목과 설명을 저장해 둔다. 빈 화면이 검색 결과에 한동안 남는다.
- 배포를 확인하려고 올린 시험용 글이 수집될 수 있다.
- 만드는 중에 글 주소 모양이 바뀌면, 수집된 옛 주소는 검색 결과에서 오류 화면으로 간다.

이슈의 조건.

- 화면 주소와 `/api` 주소가 모두 수집 금지로 응답한다.
- 설정값 하나로 켜고 끈다. 기본은 "막음"이다.
- `robots.txt`와 사이트맵은 3단계의 일이라 여기서 만들지 않는다.

## 선택지

1. **Caddy가 응답 헤더를 붙인다** (`X-Robots-Tag: noindex, nofollow`).
   - 장점: 요청이 모두 Caddy를 지나므로 한 곳에서 화면과 `/api`를 다 막는다. 헤더라서 HTML이 아닌 응답(JSON, CSS)에도 붙는다. `frontend/`를 건드리지 않는다.
   - 단점: 사람이 여는 것은 막지 않는다. 주소를 아는 사람은 본다.
2. **`robots.txt`에 `Disallow: /`를 적는다.**
   - 단점: 수집(크롤링)만 막고 검색 등록은 막지 못한다. 다른 사이트에 링크가 걸리면 내용 없이 주소만 검색에 나올 수 있다. `/api`가 "수집 금지로 응답"하는지 확인할 방법이 없다. 3단계에서 프론트가 만들 `robots.txt`와 겹친다.
3. **HTML에 `<meta name="robots" content="noindex">`를 넣는다.**
   - 단점: `frontend/`를 고쳐야 한다. JSON 응답에는 넣을 수 없다.
4. **비밀번호로 잠근다** (Caddy의 `basic_auth`).
   - 장점: 검색엔진뿐 아니라 사람도 못 들어온다. 가장 확실하다.
   - 단점: 볼 때마다 입력해야 한다. #60의 끝나는 기준("홈이 열려요", "응답에 수집 금지 표시가 붙어 있어요")과 맞지 않아 이슈를 고쳐야 한다.

## 고른 것과 이유

1번을 골랐다. 이슈가 예로 든 방법이기도 하다.

### Caddyfile

```caddyfile
# 고치기 전
{$SITE_DOMAIN} {
	encode zstd gzip

	@api path /api /api/*
	...

# 고친 뒤
{$SITE_DOMAIN} {
	encode zstd gzip

	@block-search expression `"{$BLOCK_SEARCH_ENGINES:true}" != "false"`
	header @block-search X-Robots-Tag "noindex, nofollow"

	@api path /api /api/*
	...
```

- `@block-search`는 이름 붙인 조건(matcher)이다. `header @block-search ...`는 그 조건이 참일 때만 헤더를 붙인다.
- `{$이름:기본값}`은 Caddy가 설정 파일을 읽기 전에 글자 그대로 바꿔 넣는다. 그래서 조건식 안에 써도 된다. `caddy adapt`로 바뀐 결과를 볼 수 있다.

```
BLOCK_SEARCH_ENGINES 없음   → "expr":"\"true\" != \"false\""
BLOCK_SEARCH_ENGINES=true   → "expr":"\"true\" != \"false\""
BLOCK_SEARCH_ENGINES=false  → "expr":"\"false\" != \"false\""
BLOCK_SEARCH_ENGINES=       → "expr":"\"\" != \"false\""
```

- 마지막 줄을 보면 Caddy의 기본값은 변수가 **없을 때만** 쓰인다. 빈 값으로 있으면 빈 글자가 들어간다.

확인에 쓴 명령(저장소 루트, Git Bash).

```bash
MSYS_NO_PATHCONV=1 docker run --rm -e SITE_DOMAIN=localhost -e BLOCK_SEARCH_ENGINES=false \
  -v "$(pwd -W)/infra/server/Caddyfile:/etc/caddy/Caddyfile:ro" \
  caddy:2.11 caddy adapt --config /etc/caddy/Caddyfile --validate
```

### compose.yaml

```yaml
    environment:
      SITE_DOMAIN: ${SITE_DOMAIN:?SITE_DOMAIN을 .env에 넣는다}
      BLOCK_SEARCH_ENGINES: ${BLOCK_SEARCH_ENGINES:-true}
```

- Compose의 `${이름:-기본값}`은 값이 없을 때와 **비었을 때** 모두 기본값을 쓴다. (`:` 없이 `${이름-기본값}`이면 없을 때만이다.)

### "true일 때 막음"이 아니라 "false일 때만 풀림"

조건을 `== "true"`로 쓰면 `.env`에 `ture`라고 잘못 적는 순간 풀린다. `!= "false"`로 쓰면 잘못 적어도 막힌 채로 있다. 실수로 풀리면 빈 화면이 검색에 올라가고 되돌리는 데 시간이 걸린다. 실수로 막혀 있으면 확인 명령 한 번으로 알 수 있다.

| `.env`의 값 | 헤더 |
| --- | --- |
| `true` | 붙음 |
| `false` | 붙지 않음 |
| `False` | 붙음 |
| 빈 값 | 붙음 |
| 줄 없음 | 붙음 |

## 겪은 것

### 확인한 응답

내 컴퓨터에서 `SITE_DOMAIN=localhost`로 네 가지를 띄우고 받았다. 기본값(`true`)이다.

```
/                200 text/html; charset=utf-8 | X-Robots-Tag: noindex, nofollow
/posts           200 text/html; charset=utf-8 | X-Robots-Tag: noindex, nofollow
/about           200 text/html; charset=utf-8 | X-Robots-Tag: noindex, nofollow
/no-such-page    404 text/html; charset=utf-8 | X-Robots-Tag: noindex, nofollow
/api/ping        200 application/json | X-Robots-Tag: noindex, nofollow
/api/posts       200 application/json | X-Robots-Tag: noindex, nofollow
/api/tags        200 application/json | X-Robots-Tag: noindex, nofollow
static /_next/static/chunks/0bgtdw8d1hig9.css → X-Robots-Tag: noindex, nofollow
```

`false`로 바꾸면 위 일곱 주소 모두에서 헤더가 사라졌다.

### 앱이 없을 때의 502에도 붙는다

이미지 빌드를 기다리는 동안 Caddy만 먼저 띄웠다(`up -d --no-deps caddy`). 앱이 없으니 502가 난다.

```
=== /
HTTP/1.1 502 Bad Gateway
X-Robots-Tag: noindex, nofollow
=== /api/ping
HTTP/1.1 502 Bad Gateway
X-Robots-Tag: noindex, nofollow
```

### http에서 https로 넘기는 응답에는 붙지 않는다

```
=== http → https
HTTP/1.1 308 Permanent Redirect
Location: https://localhost/
```

80번 포트의 넘기기는 Caddy가 알아서 만든 것이라 Caddyfile의 사이트 묶음 밖이다. 넘어간 뒤의 `https://` 응답에 헤더가 있어서 그대로 뒀다.

### 값을 바꾸고 restart해도 반영되지 않는다

`.env`를 `false`로 바꾸고 `docker compose restart caddy`를 했는데 헤더가 그대로 붙어 있었다.

```
### 6. restart만으로는 반영되지 않는지: false로 바꾸고 restart
  /                200 text/html; charset=utf-8 | X-Robots-Tag: noindex, nofollow
```

환경 변수는 컨테이너를 **만들 때** 들어간다. `restart`는 같은 컨테이너를 껐다 켜는 것이라 옛 값이 그대로다. `up -d`는 설정이 달라진 컨테이너를 다시 만든다.

```bash
docker compose -f infra/server/compose.yaml up -d caddy
```

```
Container konglog-frontend-1 Running ; Container konglog-backend-1 Running ; Container konglog-db-1 Running ;
Container konglog-caddy-1 Recreate ; Container konglog-caddy-1 Recreated ; Container konglog-caddy-1 Started ;
```

- Caddy만 다시 만들어졌다. 나머지 셋은 컨테이너 ID가 그대로였다.
- 끊긴 시간(0.1초마다 `/api/ping`을 불러 실패한 구간, 내 컴퓨터): 1241ms, 1040ms, 761ms, 524ms.

### 작은 실수

- 걸린 시간을 `bc`로 계산하려다 실패했다. Git Bash에는 `bc`가 없다.
  ```
  /usr/bin/bash: line 23: bc: command not found
  ```
  `date +%s%N`으로 나노초를 받아 `$(( ... ))`로 뺐다.
- Git Bash에서 `docker run -v`에 `/etc/caddy/...` 같은 경로를 주면 윈도우 경로로 바뀐다. `MSYS_NO_PATHCONV=1`을 붙이고, 내 컴퓨터 쪽 경로는 `pwd -W`로 받았다.

## 감수한 것

- 사람은 막지 않는다. 주소를 아는 사람은 만드는 중인 화면을 본다.
- 표시를 지키는 것은 검색엔진 쪽이다. 구글은 문서에 이 헤더를 지킨다고 적어 두었다. 표시를 무시하는 수집기는 막지 못한다.
- 네이버가 `X-Robots-Tag` 헤더를 지키는지는 공식 문서에서 찾지 못했다. 지키지 않으면 네이버에는 HTML의 `<meta name="robots">`가 따로 필요하고, 그것은 `frontend/`의 일이다.
- 조건을 요청마다 계산한다. 글자 두 개를 비교하는 것이라 무시할 만하다고 봤고, 따로 재지는 않았다.

확인하지 않은 것.

- 서버(konglog.my)에서의 응답. #60에서 본다.
- 검색엔진이 실제로 등록하지 않는지. 공인 인증서와 도메인이 있어야 볼 수 있다.
- 앱이 같은 헤더를 직접 보낼 때 어떻게 되는지. 지금은 프론트와 백엔드 모두 이 헤더를 보내지 않는다. 헤더가 두 번 붙을 것으로 짐작하지만 돌려 보지 않았다.
- 네이버 검색 로봇이 이 헤더를 지키는지. 2026-11-26에 검색했지만 네이버 서치어드바이저 문서에서 답을 찾지 못했다.
- `robots.txt`가 "수집만 막고 등록은 못 막는다"는 것은 문서로 읽은 내용이다. 직접 해 본 것이 아니다.

문서로 확인한 것(2026-11-26).

- 구글은 `noindex`를 보려면 그 주소를 수집할 수 있어야 한다. `robots.txt`로 수집을 막으면 `noindex`를 보지 못한다. `robots.txt`로 막힌 주소도 다른 곳에 링크가 있으면 등록될 수 있다.
  - https://developers.google.com/search/docs/crawling-indexing/block-indexing?hl=ko
  - https://developers.google.com/search/docs/crawling-indexing/robots-meta-tag?hl=ko
  - https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/X-Robots-Tag

3단계에서 풀 때 볼 것.

- 위 내용 때문에 `robots.txt`의 `Disallow`와 `noindex`를 함께 쓰면 안 된다. 막아 둔 동안에는 `robots.txt`로 수집까지 막지 않는다.

## 배운 점

- 검색엔진을 막는 방법은 "수집을 막는 것"(`robots.txt`)과 "등록을 막는 것"(`noindex`)으로 나뉜다. 숨기려면 뒤쪽이다.
- `noindex`를 HTML 밖에서 말하는 방법이 `X-Robots-Tag` 응답 헤더다. JSON이나 파일에도 쓸 수 있다.
- 켜고 끄는 설정은 "잘못 적었을 때 어느 쪽이 되는가"를 먼저 정한다. 위험한 쪽을 값 하나로만 열리게 한다.
- 기본값을 적는 문법이 Caddy(`{$이름:기본값}`)와 Compose(`${이름:-기본값}`)에서 다르고, 빈 값을 다루는 방식도 다르다. 둘 다 거치게 해서 빈 값도 "막음"이 됐다.
- 환경 변수를 바꿨으면 `restart`가 아니라 `up -d`다.

## 글로 쓸 때 강조할 것

- "검색에 나와야 하는데 왜 막나"에서 시작한다. 공개 전과 후를 나눠 설명한다.
- `robots.txt`로 막으면 될 것 같지만 안 되는 이유. 수집과 등록의 차이.
- `== "true"`와 `!= "false"`의 차이를 표로 보여 준다.
- `restart`로 안 바뀌던 장면. 컨테이너를 만들 때와 켤 때의 차이.
