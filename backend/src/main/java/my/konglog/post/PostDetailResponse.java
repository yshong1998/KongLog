package my.konglog.post;

import java.time.Instant;
import java.util.List;
import my.konglog.tag.Tag;

/**
 * 글 상세 응답.
 *
 * @param content 본문. 마크다운 원문 그대로다
 * @param editedAt 수정일. 게시한 뒤 본문을 고친 적이 없으면 null이다
 */
public record PostDetailResponse(
        String title,
        String slug,
        String summary,
        String content,
        List<String> tags,
        Instant publishedAt,
        Instant editedAt) {

    static PostDetailResponse from(Post post) {
        List<String> tagNames = post.getTags().stream().map(Tag::getName).sorted().toList();
        return new PostDetailResponse(
                post.getTitle(),
                post.getSlug(),
                post.getSummary(),
                post.getContent(),
                tagNames,
                post.getPublishedAt(),
                post.getEditedAt());
    }
}
