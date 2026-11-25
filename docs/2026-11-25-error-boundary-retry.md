---
title: 오류 화면의 "다시 시도"에 reset이 아니라 retry를 쓴 이유
date: 2026-11-25
stage: 1
type: 결정
tags: [Next.js, error.tsx, 오류 처리]
status: 메모
related: 이슈 #14, frontend/src/app/error.tsx, frontend/src/app/page.tsx, frontend/src/app/posts/page.tsx
---

## 한 줄 요약

백엔드가 꺼져서 난 오류는 서버에서 다시 받아 와야 풀린다. 그래서 오류 화면의 "다시 시도" 버튼은 `retry()`를 부른다.

## 상황

이슈 #14의 끝나는 기준은 "백엔드를 끈 상태에서는 오류 화면과 '다시 시도' 버튼이 보여요"다. 전에는 홈, 글 목록, 글 상세가 불러오기에 실패하면 화면 안에 빨간 문구 한 줄만 보였고 버튼이 없었다.

## 선택지

Next.js 16의 `error.tsx`는 두 함수를 받는다 (`node_modules/next/dist/docs/01-app/03-api-reference/03-file-conventions/error.md`).

| 함수 | 하는 일 |
| --- | --- |
| `retry()` | 오류 경계 안쪽을 서버에서 다시 받아 와서 다시 그린다 |
| `reset()` | 다시 받아 오지 않고 오류 상태만 지운 뒤 다시 그린다 |

문서 원문: "In most cases, you should use `retry()` instead." (`reset` 항목)

## 고른 것과 이유

`retry()`를 쓴다. 오류의 원인이 서버 컴포넌트에서 백엔드를 부르다 실패한 것이라, 다시 받아 오지 않는 `reset()`으로는 풀리지 않는다.

```tsx
"use client";

export default function ErrorPage({ retry }: { retry: () => void }) {
  return (
    <div role="alert" className="mx-auto flex max-w-content flex-col gap-6 pt-16">
      ...
      <button type="button" onClick={() => retry()} ...>다시 시도</button>
    </div>
  );
}
```

화면이 오류 화면에 닿도록, 글을 불러오지 못하면 던지게 바꿨다.

고치기 전 (글 목록):

```tsx
{!posts.ok ? (
  <p className="text-danger">
    글을 불러오지 못했어요. 잠시 뒤에 다시 시도해 주세요.
  </p>
) : posts.data.posts.length > 0 ? (
```

고친 뒤:

```tsx
if (!posts.ok) {
  throw new Error("글 목록을 불러오지 못했어요");
}
```

태그만 실패하면 그 화면 안에서 문구로 알린다. 글은 보여 줄 수 있으니 화면 전체를 오류로 바꾸지 않는다.

## 감수한 것

- 서버에서 난 오류의 원래 메시지는 배포 환경에서 브라우저로 넘어오지 않는다(비밀 정보 보호). 그래서 오류 화면 문구는 원인을 구분하지 않는 고정 문구다.
- 오류 화면이 헤더와 푸터를 뺀 화면 전체를 덮는다. 홈에서 글만 실패해도 홈 소개 영역까지 사라진다.

## 확인한 것과 확인하지 못한 것

- 확인함: 백엔드가 없는 주소(`API_BASE_URL=http://localhost:9`)로 띄운 서버에서 홈, 글 목록, 글 상세에 오류 화면이 나오고, "다시 시도"를 누르면 오류 화면이 그대로 남는다.
- 확인하지 못함: 백엔드가 다시 켜진 뒤 "다시 시도"로 원래 화면이 돌아오는지.
- 확인하지 못함: 예전 버전에는 `reset()`만 있었다고 기억하지만 문서로 확인하지 않았다.

## 배운 점

- 오류 경계(`error.tsx`)는 클라이언트 컴포넌트여야 한다. 루트 레이아웃의 오류는 잡지 못하고, 그건 `global-error.tsx`가 맡는다.
- "다시 시도"가 정말 다시 받아 오는지는 함수 이름만 보고 판단하면 안 된다.

## 글로 쓸 때 강조할 것

- `reset`과 `retry`의 차이를 표 하나로
- 화면 일부만 실패할 때와 전체가 실패할 때를 나눈 기준
