package my.konglog.post;

import java.time.Instant;
import java.util.List;
import my.konglog.tag.Tag;

/**
 * 글 상세 응답. 글 한 편을 읽는 데 필요한 것을 모두 담는다.
 *
 * @param content 본문. 마크다운 원문 그대로다. 화면에 맞게 바꾸는 일은 프론트엔드가 한다
 * @param tags 태그 이름들. 이름순이고, 태그가 없으면 빈 목록이다
 * @param publishedAt 발행일. 항상 UTC로 내려간다
 * @param editedAt 수정일. 게시한 뒤 본문을 고친 시각이다. 고친 적이 없으면 null이다
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
