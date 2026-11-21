package my.konglog.post;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 독자가 글을 읽는 기능. 게시 글만 다룬다. */
@Service
@Transactional(readOnly = true)
public class PostQueryService {

    private final PostQueryRepository postQueryRepository;

    public PostQueryService(PostQueryRepository postQueryRepository) {
        this.postQueryRepository = postQueryRepository;
    }

    /**
     * 게시 글 목록을 최신순으로 읽는다.
     *
     * @param tagName 이 이름의 태그가 붙은 글만 읽는다. null이면 거르지 않는다
     * @param after 이 글 다음부터 읽는다. null이면 처음부터 읽는다
     * @param size 읽을 개수
     */
    public PostListResponse getPublishedPosts(String tagName, PostCursor after, int size) {
        // 하나 더 읽어 본다. 더 읽히면 다음 글이 있다는 뜻이다.
        List<Post> found = postQueryRepository.findPublished(tagName, after, size + 1);
        boolean hasNext = found.size() > size;
        List<Post> posts = hasNext ? found.subList(0, size) : found;

        postQueryRepository.loadTags(posts);

        String nextCursor = hasNext ? PostCursor.of(posts.getLast()).encode() : null;
        long totalCount = postQueryRepository.countPublished(tagName);
        return new PostListResponse(posts.stream().map(PostSummaryResponse::from).toList(), totalCount, nextCursor);
    }

    /** 슬러그로 게시 글 하나를 읽는다. 없는 글이거나 초안이면 비어 있다. */
    public Optional<PostDetailResponse> getPublishedPost(String slug) {
        return postQueryRepository.findPublishedBySlug(slug).map(PostDetailResponse::from);
    }
}
