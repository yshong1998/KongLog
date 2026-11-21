package my.konglog.tag;

/**
 * 태그 하나와 그 태그가 붙은 게시 글의 수.
 *
 * @param name 태그 이름. 글 목록 API의 tag에 그대로 넣어 거른다
 * @param postCount 이 태그가 붙은 게시 글의 수. 초안은 세지 않는다
 */
public record TagCountResponse(String name, long postCount) {
}
