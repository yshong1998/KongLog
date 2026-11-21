/** 여러 화면이 함께 쓰는 사이트 정보. 헤더, 푸터, 페이지 제목이 여기서 읽는다. */

export const SITE_NAME = "KongLog";

export type MenuItem = {
  label: string;
  href: string;
};

/** 헤더의 주 메뉴. 순서대로 보인다. */
export const MAIN_MENU: MenuItem[] = [
  { label: "글", href: "/posts" },
  { label: "소개", href: "/about" },
];
