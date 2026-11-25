---
title: 크롬 창이 555px 밑으로 안 줄어서 iframe으로 모바일 폭을 확인했다
date: 2026-11-25
stage: 1
type: 새로 배운 것
tags: [반응형, CSS, overflow-wrap, 접근성]
status: 메모
related: 이슈 #6, frontend/src/app/layout.tsx, frontend/src/components/markdown/markdown-components.tsx
---

## 한 줄 요약

브라우저 창은 일정 폭 밑으로 줄지 않으니 같은 주소를 좁은 iframe에 띄워 검사했고, 긴 영어 단어 넘침은 `overflow-wrap: break-word`로 막았다. `anywhere`가 아니라 `break-word`여야 표가 표 안에서 스크롤된다.

## 상황

1단계 화면(홈, 글 목록, 글 상세, 소개, 페이지 없음, 오류)이 모바일 폭에서 페이지 전체 가로 스크롤 없이 보이는지 확인하는 이슈(#6).

## 알게 된 것

### 1. 크롬 창은 375px까지 줄지 않는다

Claude in Chrome의 창 크기 바꾸기로 375×812를 요청했는데 성공했다고 나오면서도 실제 `document.documentElement.clientWidth`는 555였다. 크롬 창에 최소 폭이 있다.

그래서 페이지 안에 iframe을 만들고 같은 주소를 띄워 검사했다. 같은 출처라서 iframe 안의 DOM을 바로 읽을 수 있다.

```js
const f = document.createElement('iframe');
f.style.cssText = 'width:375px;height:812px';
document.body.replaceChildren(f);
await new Promise(r => { f.onload = r; f.src = '/posts'; });
const doc = f.contentDocument.documentElement;
doc.scrollWidth > doc.clientWidth; // true면 페이지 전체 가로 스크롤이 있다
```

- iframe 안에 세로 스크롤바가 생겨서 375px로 만들어도 내용 폭은 358px였다. 더 좁은 쪽이라 검사에는 문제없다.
- 넘친 요소 찾기: 모든 요소의 `getBoundingClientRect().right`가 화면 폭보다 큰지 보고, 조상 중에 `overflow-x: auto/hidden`이 있으면 뺐다(코드 블록과 표는 안에서 스크롤하니까).
- 누르는 영역 높이: `getBoundingClientRect().height`만 보면 `::after`로 넓힌 영역을 못 센다. `getComputedStyle(el, '::after').height`도 함께 봤다.

### 2. 320px에서 긴 영어 단어가 페이지를 밀었다

375px에서는 아무 문제가 없었는데 320px에서 글 목록이 넘쳤다. 화면 폭 305, 페이지 폭 350.

```
a.after:absolute... "LazyInitializationException을 처..." right=350
```

한글은 글자 사이에서 줄을 바꿀 수 있지만 영어 단어는 띄어쓰기에서만 바꾼다. `LazyInitializationException`은 카드 안쪽 폭보다 길었다.

### 3. break-word와 anywhere의 차이

둘 다 "다른 곳에서 줄을 바꿀 수 없으면 단어 중간에서 바꾼다"인데, 차이는 최소 폭 계산이다.

- `anywhere`: 단어 중간을 줄 바꿀 수 있는 곳으로 쳐서 요소의 최소 폭(min-content)이 줄어든다.
- `break-word`: 최소 폭에는 영향이 없다.

본문의 표는 칸의 최소 폭 때문에 화면보다 넓어지고, 감싼 `div`의 `overflow-x-auto`로 안에서 스크롤된다. `anywhere`를 전체에 주면 칸의 최소 폭이 줄어 `host.docker.internal` 같은 값이 쪼개져 좁은 칸에 들어갈 수 있다(명세로 판단했고, 표에 `anywhere`를 넣어 직접 보지는 않았다). `break-word`를 넣기 전에는 표(428px)가 안에서 스크롤되는 것을 봤고, 넣은 뒤에는 표가 있는 글에 페이지 넘침이 없는 것만 확인했다. 넣은 뒤 표의 폭은 따로 재지 않았다.

iframe에서 `body`에 두 값을 각각 넣어 봤고, 카드 넘침은 둘 다 고쳐졌다(페이지 폭 305). 그래서 표에 영향이 없는 `break-word`를 골랐다.

## 써 본 방법

Tailwind 4.3에는 `wrap-break-word` 클래스가 있다.

```tsx
// 전
<body className="flex min-h-full flex-col bg-bg font-sans text-body text-ink">
// 후
<body className="flex min-h-full flex-col bg-bg font-sans text-body text-ink wrap-break-word">
```

본문 소제목 링크는 글자 높이만큼만 눌렸다(h2 36px, h3 29px). 줄 간격을 바꾸지 않으려고 누르는 영역만 `::after`로 위아래 8px씩 넓혔다(52px, 45px).

```tsx
// 전
<a href={`#${id}`} className="hover:underline">
// 후
<a
  href={`#${id}`}
  className="relative inline-block hover:underline after:absolute after:inset-x-0 after:-inset-y-2"
>
```

소개 화면의 연락처 링크(25px)는 문장 안의 링크라서 글 본문 문단 속 링크와 같게 보고 두었다.

### 오류 화면 확인

오류 화면은 백엔드가 응답하지 않을 때 나온다. 떠 있는 백엔드를 끄지 않으려고 백엔드 주소를 비어 있는 포트로 둔 서버를 하나 더 띄우려 했는데, 같은 폴더에서 `next dev`를 두 번 띄우면 막힌다.

```
⨯ Another next dev server is already running.
```

`npm run build` 뒤에 `API_BASE_URL=http://localhost:9 npx next start -p 3001`로 띄워 확인했다.

## 배운 점

- 375px만 보면 놓친다. 320px에서만 생기는 문제가 있었다.
- 넘침은 눈보다 `scrollWidth`와 요소 위치로 찾는 게 빠르다.

## 글로 쓸 때 강조할 것

- `break-word`와 `anywhere`의 차이를 표가 쪼개지는 그림으로 보여 주기.
