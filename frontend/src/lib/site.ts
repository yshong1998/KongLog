/** 여러 화면이 함께 쓰는 사이트 정보. 헤더, 푸터, 페이지 제목이 여기서 읽는다. */

export const SITE_NAME = "KongLog";

/** 블로그를 한 줄로 알리는 문구. 홈의 큰 제목과 검색 결과의 설명에 쓴다. */
export const SITE_TAGLINE = "개발하다 만난 문제를 콩알만큼씩 적어 둬요.";

/** 홈 소개 영역에 보이는 한두 줄 소개. */
export const SITE_INTRO =
  "일하다 막혔던 문제와 풀어 간 과정을 짧게 남기는 기술 블로그예요.";

/** 저작권 표기에 쓰는 연도. 블로그를 처음 만든 해. */
export const COPYRIGHT_YEAR = 2026;

export const GITHUB_URL = "https://github.com/yshong1998";

export type MenuItem = {
  label: string;
  href: string;
};

/** 헤더의 주 메뉴. 순서대로 보인다. */
export const MAIN_MENU: MenuItem[] = [
  { label: "글", href: "/posts" },
  { label: "소개", href: "/about" },
];
