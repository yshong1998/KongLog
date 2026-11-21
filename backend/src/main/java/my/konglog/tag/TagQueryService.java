package my.konglog.tag;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 독자가 태그를 읽는 기능. 게시 글이 있는 태그만 다룬다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagQueryService {

    /**
     * 글이 많은 태그가 먼저, 글 수가 같으면 이름순.
     *
     * <p>DB가 아니라 여기서 정렬한다. 이름순의 기준을 글 목록의 태그 순서와 똑같이 맞추기 위해서다.
     * DB의 글자 순서는 DB 설정에 따라 달라질 수 있다.
     */
    private static final Comparator<TagResponse> MOST_POSTS_FIRST =
            Comparator.comparingLong(TagResponse::postCount).reversed().thenComparing(TagResponse::name);

    private final TagRepository tagRepository;

    /** 게시 글이 있는 태그를 글이 많은 순으로 읽는다. */
    public TagListResponse getTagsWithPublishedPosts() {
        List<TagResponse> tags = tagRepository.findWithPublishedPostCount().stream()
                .sorted(MOST_POSTS_FIRST)
                .toList();
        return new TagListResponse(tags);
    }
}
