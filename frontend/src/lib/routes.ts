/** 화면 주소를 만드는 함수. 주소 모양을 한곳에서만 정하려고 둔다. */

/** 글 상세 화면의 주소. */
export function postPath(slug: string): string {
  return `/posts/${encodeURIComponent(slug)}`;
}

/** 글 목록 화면의 주소. */
export const POSTS_PATH = "/posts";

/** 한 태그로 거른 글 목록 화면의 주소. 태그 이름에 한글이나 띄어쓰기가 있을 수 있어 인코딩한다. */
export function tagPath(tag: string): string {
  return `${POSTS_PATH}?tag=${encodeURIComponent(tag)}`;
}

/**
 * 글 목록의 다음 묶음을 JSON으로 받는 주소. 글 목록 화면의 "더 보기"가 브라우저에서 부른다.
 * 배포 환경에서 /api로 시작하는 주소는 백엔드로 가기 때문에 /api 아래에 두지 않았다.
 */
export function morePostsPath(cursor: string, tag?: string): string {
  const params = new URLSearchParams({ cursor });
  if (tag !== undefined) {
    params.set("tag", tag);
  }
  return `/posts.json?${params}`;
}
