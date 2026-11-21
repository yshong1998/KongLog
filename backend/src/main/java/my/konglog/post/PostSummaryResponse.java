package my.konglog.post;

import java.time.Instant;
import java.util.List;
import my.konglog.tag.Tag;

/** 글 목록의 글 하나. 본문은 담지 않는다. */
public record PostSummaryResponse(String title, String slug, String summary, List<String> tags, Instant publishedAt) {

    static PostSummaryResponse from(Post post) {
        List<String> tagNames = post.getTags().stream().map(Tag::getName).sorted().toList();
        return new PostSummaryResponse(
                post.getTitle(), post.getSlug(), post.getSummary(), tagNames, post.getPublishedAt());
    }
}
