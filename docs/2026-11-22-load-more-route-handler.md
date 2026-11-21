---
title: 글 목록의 "더 보기"를 서버 함수가 아니라 Route Handler로 만든 이유
date: 2026-11-22
stage: 1
type: 결정
tags: [Next.js, Route Handler, 서버 함수, 페이지네이션]
status: 메모
related: 이슈 #12, 풀 리퀘스트 #39, 커밋 90f70f4와 b1f8327, frontend/src/app/internal/posts/route.ts, frontend/src/components/post-card-list.tsx, frontend/src/lib/routes.ts
---

## 한 줄 요약

"더 보기"로 다음 글 묶음을 받는 일은 읽기만 하는 요청이라 서버 함수 대신 Route Handler(`/internal/posts`)로 만들었다. 주소는 배포 환경의 `/api` 규칙과 글 상세의 슬러그 자리를 피해서 정했다. 처음에는 `/posts.json`으로 만들었다가 리뷰에서 바꿨다.

## 상황

이슈 #12에서 글 목록 화면(`/posts`)을 만들었다. 처음 20개는 화면을 만들 때 서버에서 읽는다. "더 보기"를 누르면 그다음 20개를 받아 목록 뒤에 붙여야 한다. 이건 이미 그려진 화면에서 브라우저가 시작하는 요청이다.

걸리는 점이 두 가지였다.

- 브라우저는 백엔드 주소를 모른다. 백엔드 주소(`API_BASE_URL`)는 Next.js 서버만 아는 값이고, 백엔드 호출은 `src/lib/api.ts`를 거치기로 했다.
- 그래서 브라우저와 백엔드 사이에 Next.js 서버가 한 번 끼어야 한다. 끼는 방법이 서버 함수와 Route Handler 두 가지다.

용어:

- 서버 함수(Server Function): `'use server'`를 붙인 함수. 브라우저에서 함수처럼 부르면 Next.js가 알아서 서버로 요청을 보낸다.
- Route Handler: `route.ts` 파일로 만드는, 화면이 아니라 데이터를 돌려주는 주소.

## 선택지

실제로 만들어 본 것은 4번과 7번이다. 나머지는 문서와 기획을 읽고 판단했다.

### 1. 서버 함수

`loadMorePosts(tag, cursor)` 같은 함수를 만들어 버튼에서 부른다.

- 장점: 주소를 정할 필요가 없다. 돌려주는 값의 타입이 브라우저 코드까지 그대로 이어진다. 코드가 가장 짧다.
- 단점: Next.js 16 문서가 이 용도로 권하지 않는다. `node_modules/next/dist/docs/01-app/01-getting-started/07-mutating-data.md` 207번째 줄에 "Server Functions are designed for server-side mutations."라고 적혀 있다. 같은 문단과 `02-guides/server-actions.md` 26~32번째 줄의 내용을 옮기면 이렇다.
  - 브라우저는 서버 함수를 한 번에 하나씩 보내고 기다린다. 여러 개를 동시에 부를 수 없다.
  - 데이터를 바꾸지 않는 요청에는 Route Handler를 쓰라고 안내한다.
- 읽기 요청인데 POST로 나간다는 점도 맞지 않는다. (문서에서 읽은 것이고 직접 확인하지 않았다.)

### 2. Route Handler를 `/api/posts`에 두기

- 단점: 배포하면 닿지 않는다. 기획 문서(기술 스택 탭)에 주소가 `/api`로 시작하면 Spring Boot로, 나머지는 Next.js로 간다고 적혀 있다. 로컬에서는 되고 배포하면 백엔드로 가 버린다.

### 3. Route Handler를 `/posts/more`에 두기

- `/posts`에는 둘 수 없다. `page.tsx`와 `route.ts`는 같은 폴더에 함께 있을 수 없다(`01-getting-started/15-route-handlers.md` 39번째 줄).
- 단점: 이슈 #13에서 글 상세(`/posts/{슬러그}`)가 생기면 슬러그가 `more`인 글의 주소와 겹친다. 정해진 주소가 슬러그 자리보다 먼저 잡힌다고 알고 있어서, 그런 글은 열리지 않게 된다. (글 상세가 아직 없어서 직접 확인하지 못했다.)

### 4. Route Handler를 `/posts.json`에 두기

폴더 이름에 점을 넣을 수 있다. 문서의 `app/rss.xml/route.ts` 예시와 같은 방식이다.

- 장점: `/api`로 시작하지 않는다. `/posts/` 아래가 아니라서 슬러그와 겹치지 않는다. "글 목록의 JSON 판"이라는 뜻이 주소에 보인다.
- 단점: 브라우저 → Next.js → 백엔드로 한 번 더 거친다. 폴더 이름에 점을 넣는 방식은 `rss.xml`, `sitemap.xml`처럼 이름이 정해진 주소에 주로 쓴다. 화면이 부르는 데이터 주소에 쓰는 것은 Next.js 프로젝트에서 흔한 모양이 아니다.

### 5. 브라우저가 백엔드의 `/api/posts`를 바로 부르기

배포 환경에서는 같은 도메인의 `/api`가 백엔드로 가므로 가능하다.

- 단점: 로컬에는 그 길을 나눠 주는 서버(Caddy)가 없다. 브라우저는 3000번, 백엔드는 8080번이라 `next.config.ts`에 주소를 넘겨주는 설정을 넣거나 백엔드가 다른 출처의 요청을 허용해야 한다. 둘 다 이 이슈의 범위 밖이고, 백엔드의 허용 설정은 프론트가 아는 내용이 아니다. 시도하지 않았다.

### 6. 받은 개수를 주소에 남기기

"더 보기"를 `/posts?count=40` 같은 링크로 만들고 서버가 그만큼 읽는다.

- 장점: 새로 고쳐도, 주소를 공유해도 받은 만큼 보인다.
- 단점: 백엔드는 한 번에 20개까지만 준다. 40개를 그리려면 서버가 두 번, 60개면 세 번을 차례로 불러야 한다. 주소가 바뀔 때 목록 전체가 대기 문구로 바뀌었다 돌아올 수 있다. 시도하지 않았다.

### 7. Route Handler를 `/internal/posts`에 두기

프론트의 화면이 부르는 데이터 주소를 모아 두는 폴더(`app/internal/`)를 따로 만든다.

- 장점: 4번과 같은 이유로 `/api`, 슬러그와 겹치지 않는다. 폴더 구조가 `app/api/...`를 쓰는 흔한 모양과 닮았다. 뒤 단계에서 이런 주소가 더 생겨도 한곳에 모인다.
- 단점: 지금은 주소가 하나뿐인데 폴더가 한 겹 늘어난다. 이름이 `internal`이어도 막혀 있는 주소는 아니다. 누구나 부를 수 있다.

### 8. 프론트가 `/api`를 쓰고 백엔드의 주소를 바꾸기

흔한 관례대로 `app/api/posts/route.ts`를 쓰려면 백엔드가 `/api`를 내줘야 한다. 방법은 세 가지를 따져 봤고, 모두 시도하지 않았다.

- 백엔드 주소의 앞부분을 다른 이름으로 바꾸기: 바깥에서 보면 API가 두 군데로 나뉜다.
- 백엔드를 다른 도메인(`api.konglog.my`)으로 빼기: 기획에서 같은 도메인으로 묶은 이유(로그인 쿠키와 허용 설정이 단순함)를 잃는다.
- 백엔드를 바깥에 열지 않고 Next.js가 전부 받아 넘기기: 모든 API 요청이 Next.js를 거친다. 기획은 좋아요, 댓글, 로그인을 브라우저가 백엔드에 직접 부르기로 했는데 그 결정을 뒤집는다.

얻는 것은 주소 하나의 이름인데 백엔드, 배포 설정, 기획 문서를 모두 고쳐야 해서 하지 않기로 했다.

## 고른 것과 이유

7번. 문서가 읽기 요청에 권하는 방식이고, 주소가 배포 규칙과 글 상세 주소 어느 쪽과도 부딪히지 않는다.

처음에는 4번(`/posts.json`)으로 만들어 풀 리퀘스트 #39에 올렸다(커밋 90f70f4). 리뷰에서 "폴더 이름을 이렇게 짓는 구조는 처음 봤다"는 말이 나왔고, 8번까지 따져 본 뒤 `/api`는 백엔드에 그대로 두고 7번으로 바꿨다(커밋 b1f8327). 바꾼 것은 폴더 위치와 주소를 만드는 함수(`morePostsPath`)의 한 줄이다.

Route Handler는 이어 받기만 받는다. 처음 20개는 화면이 직접 읽으므로 `cursor`가 없는 요청은 400으로 돌려보낸다.

```ts
// frontend/src/app/internal/posts/route.ts
export async function GET(request: NextRequest) {
  const params = request.nextUrl.searchParams;
  const cursor = params.get("cursor");
  if (!cursor) {
    return Response.json({ message: "cursor가 필요해요" }, { status: 400 });
  }

  const result = await getPosts({
    tag: params.get("tag") || undefined,
    cursor,
  });
  if (!result.ok) {
    // 백엔드에 닿지 못했거나 백엔드가 오류를 돌려준 경우다.
    return Response.json(
      { message: "글을 불러오지 못했어요" },
      { status: 502 },
    );
  }
  return Response.json(result.data);
}
```

브라우저 쪽은 받은 글과 다음 커서를 상태로 들고 있다가 뒤에 붙인다.

```tsx
// frontend/src/components/post-card-list.tsx
async function loadMore() {
  if (cursor === null || status === "loading") {
    return;
  }
  setStatus("loading");
  const more = await fetchMorePosts(cursor, tag);
  if (more === undefined) {
    setStatus("failed");
    return;
  }
  setPosts((current) => [...current, ...more.posts]);
  setCursor(more.nextCursor);
  setStatus("idle");
}
```

태그가 바뀌면 받아 둔 글을 버려야 한다. 화면에서 `<PostCardList key={tag ?? ""} ...>`처럼 `key`를 태그로 줘서 태그가 바뀔 때 상태가 처음부터 다시 시작하게 했다.

### 직접 확인한 것

로컬 백엔드와 테스트용 글(게시 24개)로 확인했다. 주소를 바꾼 뒤에 다시 부른 결과다.

```
$ curl 'http://localhost:3000/internal/posts'
{"message":"cursor가 필요해요"}                    → 400

$ curl 'http://localhost:3000/internal/posts?cursor=bad'
{"message":"글을 불러오지 못했어요"}                 → 502

$ curl 'http://localhost:3000/internal/posts?cursor=MjAyNi0wOS0xNVQwMDowMDowMFosMzE'
글 4개, nextCursor는 null, totalCount는 24          → 200

$ curl 'http://localhost:3000/posts.json?cursor=MjAyNi0wOS0xNVQwMDowMDowMFosMzE'
                                                   → 404 (옛 주소)
```

- 브라우저에서 "더 보기"를 누르면 요청이 `/internal/posts?cursor=MjAyNi0wOS0xNVQwMDowMDowMFosMzE` 하나만 나갔다. 불러오는 동안 한 번 더 눌러도 늘지 않는 것은 주소를 바꾸기 전(`/posts.json`)에 확인했다.
- 21번째부터 24번째 글이 뒤에 붙었고, 24개의 주소가 모두 달랐다(겹친 글 없음). 다 받은 뒤에는 버튼이 사라졌다.
- 응답을 502로 바꿔치기하면 "글을 더 불러오지 못했어요."와 "다시 시도"가 보였고, 다시 누르면 이어서 받았다. (주소를 바꾸기 전에 확인했다. 바꾼 뒤에는 다시 해 보지 않았다.)

백엔드에 닿지 않는 주소로 빌드한 결과(`API_BASE_URL=http://localhost:9 npm run build`):

```
Route (app)
┌ ◐ /
├ ○ /_not-found
├ ○ /icon.svg
├ ƒ /internal/posts
└ ◐ /posts

○  (Static)             prerendered as static content
◐  (Partial Prerender)  prerendered as static HTML with dynamic server-streamed content
ƒ  (Dynamic)            server-rendered on demand
```

`/internal/posts`는 요청의 주소를 읽기 때문에 빌드할 때 미리 만들어지지 않고 요청마다 실행된다(ƒ). 그래서 빌드는 백엔드 없이도 된다.

## 감수한 것

- 더 보기 한 번에 요청이 두 단계를 거친다(브라우저 → Next.js → 백엔드).
- 받아 둔 글은 주소에 남지 않는다. 새로 고치거나 주소를 공유하면 처음 20개부터다.
- 브라우저는 `/internal/posts`의 응답 모양을 다시 확인하지 않는다. 모양 확인은 `getPosts`가 서버에서 이미 했고, 같은 프로젝트의 주소라서 믿기로 했다.
- `/internal/posts`는 누구나 부를 수 있는 주소다. 이름이 `internal`이어도 막아 주는 것은 없다. 돌려주는 것은 백엔드의 글 목록 API와 같은 게시 글뿐이라 새로 드러나는 정보는 없다.

다시 검토할 조건:

- 6단계에서 검색과 정렬이 생기면 `/internal/posts`가 넘겨받는 값이 늘어난다. 그때 값이 많아지면 5번(백엔드를 바로 부르기)을 다시 본다.
- 좋아요와 댓글은 기획상 브라우저가 API를 직접 부른다. 그 길이 로컬에서도 열리면(이슈 #38, 백엔드에 CORS 설정 추가) `/internal/posts` 없이 `/api/posts`를 바로 부를 수 있는지 다시 본다. 그러면 이 주소와 폴더를 지울 수 있다.
- 이슈 #13에서 글 상세가 생기면 3번의 "슬러그와 겹친다"는 판단이 맞았는지 확인한다.

## 배운 점

- 서버 함수는 "브라우저에서 서버 코드를 부르는 가장 쉬운 방법"이지만 Next.js 문서는 데이터를 바꾸는 일에 쓰라고 한다. 읽기는 서버 컴포넌트나 Route Handler의 몫이다.
- Route Handler의 주소는 프론트 안에서만 정하면 안 된다. 배포할 때 앞단 서버가 주소를 어떻게 나누는지(`/api`는 백엔드)까지 봐야 한다. 로컬에서 되는 주소가 배포하면 다른 서버로 갈 수 있다.
- 폴더 이름에 점을 넣어 `posts.json` 같은 주소를 만들 수 있다. 다만 되는 것과 흔한 것은 다르다. 처음 보는 사람이 폴더 구조를 읽고 바로 알 수 있는지도 고르는 기준이다.
- Route Handler 폴더를 옮긴 뒤 타입 검사가 아래 오류로 실패했다. 개발 서버가 만들어 둔 타입 파일(`.next/dev/types/`)에 옛 경로가 남아 있어서였다. `tsconfig.json`이 그 폴더를 읽는다. `.next/dev/types`를 지우고 다시 돌리니 통과했다. 자동 검사는 매번 새로 받아서 돌리므로 영향이 없다.

  ```
  .next/dev/types/validator.ts(92,39): error TS2307: Cannot find module '../../../src/app/posts.json/route.js' or its corresponding type declarations.
  ```
- 캐시 컴포넌트를 켠 상태에서는 다른 화면에 갔다가 뒤로 가기로 돌아와도 상태가 남는다. 24개를 받고 홈에 갔다 돌아오니 24개가 그대로였다. 문서(`02-guides/preserving-ui-state.md`)에 따르면 화면을 지우지 않고 숨겨 두기 때문이고, 3개 화면까지 남겨 둔다.
- 그래서 "태그가 바뀌면 처음부터"는 저절로 되지 않는다. `key`로 직접 정해 줘야 한다.

## 글로 쓸 때 강조할 것

- "브라우저가 백엔드 주소를 모른다"에서 출발해 선택지가 갈리는 과정.
- 주소 하나를 정하는 데 걸린 조건 세 가지: `page.tsx`와 같은 폴더 불가, 배포 환경의 `/api` 규칙, 글 상세의 슬러그 자리.
- `/posts.json`에서 `/internal/posts`로 바꾼 과정. 조건은 다 맞췄는데도 "처음 보는 구조"라는 이유로 바꿨다.
- 서버 함수로 만들었으면 코드가 더 짧았다는 점을 숨기지 않기. 짧은 쪽을 고르지 않은 이유가 글의 핵심이다.
