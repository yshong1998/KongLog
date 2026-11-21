package my.konglog.tag;

/**
 * 태그 목록의 태그 하나.
 *
 * @param name 태그 이름. 글 목록의 태그 필터(tag)에 그대로 넣는 값이다
 * @param postCount 이 태그가 붙은 게시 글의 수. 초안은 세지 않는다
 */
public record TagResponse(String name, long postCount) {
}
