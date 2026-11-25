package my.konglog.post;

/** 글 하나에 붙은 태그 이름 하나. 글 목록의 태그를 한 번에 읽을 때 쓴다. */
public record PostTagName(Long postId, String tagName) {
}
