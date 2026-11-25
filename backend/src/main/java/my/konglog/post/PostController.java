package my.konglog.post;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** 독자가 글을 읽는 API. 게시 글만 내려준다. */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    /** 한 번에 내려주는 글의 최대 개수이자 기본 개수. */
    static final int MAX_SIZE = 20;

    private final PostQueryService postQueryService;

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
        return postQueryService.getPublishedPosts(tagName(tag), decodeCursor(cursor), size);
    }

    /**
     * 글 상세. 게시 글 하나를 본문과 함께 내려준다.
     *
     * <p>없는 글과 초안은 똑같이 404다. 초안이 있다는 사실이 밖에서 보이면 안 된다.
     */
    @GetMapping("/{slug}")
    public PostDetailResponse detail(@PathVariable String slug) {
        return postQueryService
                .findPublishedPost(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "글을 찾을 수 없다"));
    }

    /**
     * 거를 태그 이름. 비어 있으면 거르지 않는다는 뜻으로 null이다.
     *
     * <p>줄바꿈이나 null 글자 같은 제어 문자는 태그 이름에 쓸 수 없어서 400이다. null 글자는 DB가 받지 못해
     * 그대로 넘기면 500이 난다.
     */
    private String tagName(String tag) {
        if (tag == null || tag.isBlank()) {
            return null;
        }
        if (tag.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tag에 쓸 수 없는 글자가 있다");
        }
        return tag;
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
