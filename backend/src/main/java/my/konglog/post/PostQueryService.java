package my.konglog.post;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        List<PostSummaryRow> found =
                postRepository.findPublished(tagName, afterPublishedAt, afterId, Limit.of(size + 1));
        boolean hasNext = found.size() > size;
        List<PostSummaryRow> rows = hasNext ? found.subList(0, size) : found;

        Map<Long, List<String>> tagNamesByPostId = rows.isEmpty()
                ? Map.of()
                : postRepository.findTagNames(rows.stream().map(PostSummaryRow::id).toList()).stream()
                        .collect(groupingBy(PostTagName::postId, mapping(PostTagName::tagName, toList())));

        List<PostSummaryResponse> posts = rows.stream()
                .map(row -> PostSummaryResponse.of(row, tagNamesByPostId.getOrDefault(row.id(), List.of())))
                .toList();
        String nextCursor = hasNext ? PostCursor.of(rows.getLast()).encode() : null;
        long totalCount = postRepository.countPublished(tagName);
        return new PostListResponse(posts, totalCount, nextCursor);
    }

    /**
     * 게시 글 하나를 슬러그로 읽는다.
     *
     * @return 없는 글이거나 초안이면 비어 있다
     */
    public Optional<PostDetailResponse> findPublishedPost(String slug) {
        return postRepository.findPublishedBySlug(slug).map(PostDetailResponse::from);
    }
}
