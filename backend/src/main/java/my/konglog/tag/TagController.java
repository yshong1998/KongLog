package my.konglog.tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 독자가 태그를 읽는 API. 태그 필터와 홈의 태그 모음에 쓴다. */
@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagQueryRepository tagQueryRepository;

    public TagController(TagQueryRepository tagQueryRepository) {
        this.tagQueryRepository = tagQueryRepository;
    }

    /** 태그 목록. 게시 글이 있는 태그만, 글이 많은 순으로 내려준다. */
    @GetMapping
    public TagListResponse list() {
        return new TagListResponse(tagQueryRepository.findTagsWithPublishedPostCount());
    }
}
