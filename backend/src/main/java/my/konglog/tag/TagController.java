package my.konglog.tag;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 독자가 태그를 읽는 API. 태그 필터와 홈의 태그 모음에 쓴다. */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagQueryService tagQueryService;

    /** 태그 목록. 게시 글이 있는 태그만, 글이 많은 순으로 내려준다. 받는 값은 없다. */
    @GetMapping
    public TagListResponse list() {
        return tagQueryService.getTagsWithPublishedPosts();
    }
}
