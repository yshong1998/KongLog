package my.konglog.post;

import java.time.Instant;

/**
 * 글 목록에 쓰는 글 한 줄. 목록에 필요한 칸만 DB에서 읽는다.
 *
 * <p>엔티티로 읽으면 목록에 쓰지 않는 본문까지 매번 읽게 된다. 그래서 목록은 이 모양으로 따로 읽는다.
 */
public record PostSummaryRow(Long id, String title, String slug, String summary, Instant publishedAt) {
}
