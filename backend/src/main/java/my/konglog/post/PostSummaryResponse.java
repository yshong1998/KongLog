package my.konglog.post;

import java.time.Instant;
import java.util.List;

/**
 * 글 목록의 글 하나. 본문은 담지 않는다.
 *
 * @param tags 태그 이름들. 이름순이고, 태그가 없으면 빈 목록이다
 * @param publishedAt 발행일. 항상 UTC로 내려간다
 */
public record PostSummaryResponse(String title, String slug, String summary, List<String> tags, Instant publishedAt) {

    /** @param tagNames 이 글의 태그 이름들. 순서는 상관없고, 여기서 이름순으로 맞춘다 */
    static PostSummaryResponse of(PostSummaryRow row, List<String> tagNames) {
        return new PostSummaryResponse(
                row.title(), row.slug(), row.summary(), tagNames.stream().sorted().toList(), row.publishedAt());
    }
}
