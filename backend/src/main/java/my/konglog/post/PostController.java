package my.konglog.post;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** 독자가 글을 읽는 API. 게시 글만 내려준다. */
@RestController
@RequestMapping("/api/posts")
public class PostController {

    /** 한 번에 내려주는 글의 최대 개수이자 기본 개수. */
    static final int MAX_SIZE = 20;

    private final PostQueryService postQueryService;

    public PostController(PostQueryService postQueryService) {
        this.postQueryService = postQueryService;
    }

    /**
     * 글 목록. 게시 글만 발행일 최신순으로 내려준다.
     *
     * @param tag 태그 이름. 이 태그가 붙은 글만 내려준다. 없거나 비어 있으면 거르지 않는다
     * @param cursor 앞선 응답의 nextCursor. 그다음 글부터 내려준다. 없으면 처음부터다
     * @param size 받을 개수. 1부터 20까지, 기본 20
     */
    @GetMapping
    public PostListResponse list(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "" + MAX_SIZE) int size) {
        if (size < 1 || size > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size는 1부터 " + MAX_SIZE + "까지 넣을 수 있다");
        }
        String tagName = (tag == null || tag.isBlank()) ? null : tag;
        return postQueryService.getPublishedPosts(tagName, decodeCursor(cursor), size);
    }

    private PostCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return PostCursor.decode(cursor);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cursor 값이 올바르지 않다", e);
        }
    }
}
