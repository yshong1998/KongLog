/*
 * 백엔드는 시각을 UTC로 준다. 시간대를 정하지 않고 날짜로 바꾸면
 * 한국 시간 자정부터 오전 9시 사이에 게시한 글이 하루 전 날짜로 보인다.
 */
const DATE_FORMAT = new Intl.DateTimeFormat("ko-KR", {
  timeZone: "Asia/Seoul",
  year: "numeric",
  month: "numeric",
  day: "numeric",
});

/** 시각을 한국 시간 기준 날짜로 바꾼다. 예: "2026-11-18T00:00:00Z" → "2026. 11. 18." */
export function formatDate(iso: string): string {
  return DATE_FORMAT.format(new Date(iso));
}
