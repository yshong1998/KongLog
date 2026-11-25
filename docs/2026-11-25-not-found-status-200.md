---
title: 없는 글인데 상태 코드가 200으로 나간 이유
date: 2026-11-25
stage: 1
type: 새로 배운 것
tags: [Next.js, 스트리밍, notFound, SEO]
status: 메모
related: 이슈 #14, frontend/src/app/posts/[slug]/page.tsx, frontend/src/app/not-found.tsx
---

## 한 줄 요약

스트리밍이 시작된 뒤에 `notFound()`를 부르면 화면은 "페이지를 찾을 수 없음"이지만 상태 코드는 200이다. 대신 Next.js가 `noindex`를 붙여서 검색에는 나오지 않는다.

## 상황

이슈 #14에서 "페이지를 찾을 수 없음" 화면(`app/not-found.tsx`)을 만들고, 초안 글 주소로 확인하던 중이었다.

## 알게 된 것

직접 확인한 것 (로컬 개발 서버, 2026-11-25):

```
$ curl -s -o /dev/null -w "%{http_code}\n" localhost:3000/posts/draft-kubernetes-notes
200
$ curl -s localhost:3000/posts/draft-kubernetes-notes | grep -o '<meta name="robots"[^>]*>'
<meta name="robots" content="noindex"/>
$ curl -s -o /dev/null -w "%{http_code}\n" localhost:3000/nope
404
```

- 어느 주소에도 맞지 않는 `/nope`은 404다. 그리는 일 없이 라우팅 단계에서 끝나서다.
- 글 상세는 주소의 슬러그를 요청 때 읽어야 해서 `<Suspense>` 안에서 글을 불러온다. 그래서 헤더와 "글을 불러오고 있어요" 문구가 먼저 나가고, 이때 200이 함께 나간다. 글이 없다는 걸 알고 `notFound()`를 부를 때는 이미 상태 코드를 바꿀 수 없다.

Next.js 16 문서(`node_modules/next/dist/docs/01-app/03-api-reference/03-file-conventions/not-found.md`) 원문:

> Before streaming starts, Next.js returns a `404` status code. After streaming starts, the status code remains `200` and the not-found UI renders in the response. Next.js injects a `<meta name="robots" content="noindex" />` tag in both cases.

## 써 본 방법

브라우저 탭 제목도 "페이지를 찾을 수 없음"으로 맞췄다. 메타데이터는 `generateMetadata`가 따로 정해서, 처음에는 없는 글의 제목이 그냥 "KongLog"였다.

고치기 전:

```tsx
const loaded = await loadPost(slug).catch(() => null);
return loaded === null
  ? {}
  : { title: loaded.post.title, description: loaded.post.summary };
```

고친 뒤:

```tsx
const loaded = await loadPost(slug).catch(() => undefined);
if (loaded === undefined) {
  return {};
}
if (loaded === null) {
  return { title: "페이지를 찾을 수 없음" };
}
return { title: loaded.post.title, description: loaded.post.summary };
```

## 확인하지 못한 것

- 배포 환경(`next start`)에서 초안 주소의 상태 코드와 `noindex`는 확인하지 않았다.
- 404를 꼭 내려야 한다면 스트리밍 전에 글 유무를 알아야 한다. 캐시 컴포넌트에서 그렇게 할 방법은 찾아보지 않았다.

## 배운 점

- 상태 코드는 응답 맨 앞에 붙어서, 스트리밍을 시작한 뒤에는 바꿀 수 없다.
- 화면이 404처럼 보여도 상태 코드는 다를 수 있다. 확인은 `curl -w "%{http_code}"`로 한다.

## 글로 쓸 때 강조할 것

- "화면은 404인데 상태 코드는 200"이라는 의외성
- 그래도 괜찮은 이유: `noindex`로 검색에 나오지 않는다. 초안이 밖에 드러나지 않아야 한다는 기획 조건과 연결된다.
