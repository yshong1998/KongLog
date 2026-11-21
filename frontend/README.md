# KongLog 프론트엔드

Next.js 16(App Router), TypeScript, Tailwind CSS로 만든 화면입니다.

## 실행

```bash
npm install
cp .env.example .env.local
npm run dev
```

http://localhost:3000 에서 확인합니다. 백엔드(8080 포트)가 떠 있으면 홈에 최근 글과 태그가 보이고, 꺼져 있으면 "글을 불러오지 못했어요"가 보입니다.

## 명령

| 명령 | 하는 일 |
| --- | --- |
| `npm run dev` | 개발 서버 |
| `npm run build` | 배포용 빌드 |
| `npm run lint` | 코드 검사 |
| `npm run typecheck` | 타입 검사 |

## 디자인 시스템

색, 글자, 모서리, 폭은 `src/app/globals.css`에 있습니다.

- 색: `bg-surface`, `text-ink-muted`처럼 디자인 시스템 이름만 씁니다. Tailwind 기본 색(`bg-green-500` 등)은 지워 두었습니다.
- 글자: `text-h1`, `text-h2`, `text-h3`, `text-body`, `text-small`, `text-code`. 블로그 이름만 `font-display text-wordmark`.
- 간격: 1, 2, 4, 6, 10, 16 여섯 단계만 씁니다. 예: `p-6`, `gap-2`.
- 모서리: 누르는 작은 것은 `rounded-pill`, 내용을 담는 것은 `rounded-md`.

글꼴은 `@fontsource` 패키지로 함께 배포합니다. 빌드할 때 외부 글꼴 서버에 접속하지 않습니다.

## Next.js 버전 주의

이 프로젝트는 캐시 컴포넌트(`cacheComponents`)를 켜고 있습니다. 요청마다 새로 가져와야 하는 데이터는 `<Suspense>` 안에서 읽습니다. 자세한 내용은 `node_modules/next/dist/docs/`의 문서를 봅니다.
