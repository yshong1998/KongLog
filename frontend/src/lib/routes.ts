/** 화면 주소를 만드는 함수. 주소 모양을 한곳에서만 정하려고 둔다. */

/** 글 상세 화면의 주소. */
export function postPath(slug: string): string {
  return `/posts/${encodeURIComponent(slug)}`;
}

/** 한 태그로 거른 글 목록 화면의 주소. 태그 이름에 한글이나 띄어쓰기가 있을 수 있어 인코딩한다. */
export function tagPath(tag: string): string {
  return `/posts?tag=${encodeURIComponent(tag)}`;
}
