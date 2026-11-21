---
title: 홈의 글과 태그를 캐시하지 않고 요청마다 읽기로 한 이유
date: 2026-11-22
stage: 1
type: 결정
tags: [Next.js, 캐시, 빌드]
status: 메모
related: 이슈 #11, frontend/src/app/page.tsx, frontend/src/lib/api.ts, .github/workflows/ci.yml
---

## 한 줄 요약

홈의 최근 글과 태그 모음은 `'use cache'`로 캐시하지 않고 `<Suspense>` 안에서 요청마다 읽는다. 캐시를 쓰면 빌드하는 순간의 백엔드 응답이 화면에 굳는데, 자동 검사는 백엔드 없이 프론트를 빌드하기 때문이다.

## 상황

이슈 #11에서 홈 화면을 만들었다. 홈은 `GET /api/posts?size=5`와 `GET /api/tags`를 읽어 최근 글 5개와 태그 모음을 보여 준다.

이 프로젝트는 Next.js 16의 캐시 컴포넌트(`cacheComponents: true`)를 켜 두었다. 이 방식에서는 데이터를 읽는 컴포넌트마다 둘 중 하나를 골라야 한다.

- `'use cache'`를 붙인다. 결과가 캐시되고, 빌드할 때 미리 만든 HTML에 들어간다.
- `<Suspense>`로 감싼다. 미리 만든 HTML에는 대기 문구만 들어가고, 데이터는 요청이 올 때 읽는다.

블로그 글은 자주 바뀌지 않으니 처음에는 캐시가 맞아 보였다. Next.js 문서의 `cacheLife` 설명에도 `days`의 쓰임새가 "blog posts, articles"로 적혀 있다.

자동 검사(`.github/workflows/ci.yml`)의 프론트엔드 작업은 백엔드를 띄우지 않고 빌드한다.

```yaml
      - run: npm ci
      - run: npm run lint
      - run: npm run typecheck
      - run: npm run build
```

## 선택지

세 가지 모두 백엔드에 닿지 않는 주소(`API_BASE_URL=http://localhost:9`)로 `npm run build`를 돌려 직접 확인했다.

### 1. 캐시하고, 읽기에 실패하면 안내 문구 보여 주기

```tsx
async function RecentPostsAndTags() {
  "use cache";
  cacheLife("hours");

  const [posts, tags] = await Promise.all([
    getPosts({ size: RECENT_POST_COUNT }),
    getTags(),
  ]);
  // posts.ok가 false면 "글을 불러오지 못했어요"를 그린다
```

빌드는 성공했다. 홈은 완전히 미리 만든 화면(○)이 됐다.

```
Route (app)      Revalidate  Expire
┌ ○ /                    1h      1d
├ ○ /_not-found
└ ○ /icon.svg
```

그런데 미리 만든 HTML(`.next/server/app/index.html`)에 실패 문구가 그대로 들어가 있었다.

```
글을 불러오지 못했어요. 잠시 뒤에 다시 시도해 주세요.
태그를 불러오지 못했어요. 잠시 뒤에 다시 시도해 주세요.
```

- 장점: 요청마다 백엔드를 부르지 않는다. 첫 화면에 글이 바로 들어 있다.
- 단점: 빌드할 때 백엔드가 꺼져 있으면 실패 문구가 캐시된다. 빌드는 성공으로 끝나서 알아채기 어렵다. 위 설정이면 다시 만들어질 때까지 1시간 동안 그 문구가 나간다.

### 2. 캐시하고, 읽기에 실패하면 오류 던지기

```tsx
  if (!posts.ok) {
    throw new Error("글 목록을 읽지 못했다");
  }
```

빌드가 실패했다.

```
Error occurred prerendering page "/". Read more: https://nextjs.org/docs/messages/prerender-error
Error: 글 목록을 읽지 못했다
    at r (src/app/page.tsx:49:11)
Export encountered an error on /page: /, exiting the build.
⨯ Next.js build worker exited with code: 1 and signal: null
```

- 장점: 잘못된 화면이 캐시될 일이 없다.
- 단점: 빌드하려면 백엔드가 떠 있어야 한다. 자동 검사에 백엔드와 DB를 함께 띄워야 하고, 프론트만 고친 풀 리퀘스트도 백엔드 상태에 따라 실패한다.

### 3. 캐시하지 않고 `<Suspense>` 안에서 요청마다 읽기

```tsx
<Suspense fallback={<p className="text-ink-muted">글을 불러오고 있어요.</p>}>
  <RecentPostsAndTags />
</Suspense>
```

빌드는 성공했고, 홈은 일부만 미리 만든 화면(◐)이 됐다. 미리 만든 HTML에는 헤더, 소개 영역, "글을 불러오고 있어요."까지만 들어간다.

```
Route (app)
┌ ◐ /
├ ○ /_not-found
└ ○ /icon.svg

○  (Static)             prerendered as static content
◐  (Partial Prerender)  prerendered as static HTML with dynamic server-streamed content
```

- 장점: 빌드가 백엔드와 무관하다. 글을 게시하면 다음 요청부터 바로 보인다.
- 단점: 요청마다 백엔드를 두 번 부른다. 글이 잠깐 늦게 나타난다.

## 고른 것과 이유

3번. 지금은 빌드가 백엔드 없이 돌아가는 것이 더 중요하다. 1번은 조용히 틀린 화면을 내보낼 수 있고, 2번은 자동 검사 구성을 바꿔야 한다. 방문자가 적은 개인 블로그라 요청마다 읽는 비용은 아직 문제가 아니다.

## 감수한 것

- 홈을 열 때마다 백엔드 호출이 두 번 나간다.
- 백엔드가 느리면 그만큼 글이 늦게 보인다. 호출마다 5초까지만 기다리고 그 뒤에는 실패 문구를 보여 준다.

다시 검토할 조건:

- 배포 환경이 정해져서 빌드할 때 백엔드에 닿을 수 있게 되면 2번을 다시 본다.
- 3단계에서 글 게시 기능이 생기면, 게시할 때 캐시를 바로 지우는 방법(`cacheTag`, `revalidateTag`)과 함께 캐시를 다시 본다. 이 방법은 아직 써 보지 않았다.

## 배운 점

- `'use cache'`는 빌드하는 순간의 결과를 굳힌다. 실패를 오류로 던지지 않고 값으로 돌려주면 실패한 화면도 정상 결과처럼 캐시된다.
- 빌드 결과 표의 기호로 확인할 수 있다. ○는 전부 미리 만든 화면, ◐는 일부만 미리 만들고 나머지는 요청 때 채우는 화면이다.
- 실제로 무엇이 미리 만들어졌는지는 `.next/server/app/index.html`을 열어 보면 된다.

## 글로 쓸 때 강조할 것

- "빌드가 성공했는데 화면이 틀렸다"는 1번의 결과. 오류를 삼키는 코드와 캐시가 만나면 생기는 일이다.
- 세 선택지의 빌드 결과 표를 나란히 보여 주기.
