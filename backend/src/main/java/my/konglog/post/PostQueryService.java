package my.konglog.post;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 독자가 글을 읽는 기능. 게시 글만 다룬다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

    private final PostRepository postRepository;

    /**
     * 게시 글 목록을 최신순으로 읽는다.
     *
     * @param tagName 이 이름의 태그가 붙은 글만 읽는다. null이면 거르지 않는다
     * @param after 이 글 다음부터 읽는다. null이면 처음부터 읽는다
     * @param size 읽을 개수
     */
    public PostListResponse getPublishedPosts(String tagName, PostCursor after, int size) {
        Instant afterPublishedAt = after == null ? null : after.publishedAt();
        Long afterId = after == null ? null : after.id();

        // 하나 더 읽어 본다. 더 읽히면 다음 글이 있다는 뜻이다.
        List<Post> found = postRepository.findPublished(tagName, afterPublishedAt, afterId, Limit.of(size + 1));
        boolean hasNext = found.size() > size;
        List<Post> posts = hasNext ? found.subList(0, size) : found;

        // 결과는 받지 않는다. 같은 트랜잭션 안에서는 같은 글이 같은 객체라서, 위에서 읽은 글에 태그가 채워진다.
        if (!posts.isEmpty()) {
            postRepository.findWithTags(posts);
        }

        String nextCursor = hasNext ? PostCursor.of(posts.getLast()).encode() : null;
        long totalCount = postRepository.countPublished(tagName);
        return new PostListResponse(posts.stream().map(PostSummaryResponse::from).toList(), totalCount, nextCursor);
    }
}
