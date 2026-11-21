package my.konglog.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import my.konglog.tag.Tag;
import my.konglog.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * GET /api/posts 를 실제 DB에 대고 확인한다.
 * 테스트마다 트랜잭션을 되돌리므로 DB에 데이터가 남지 않는다.
 */
@SpringBootTest
@Transactional
class PostListApiTest {

    /** 기준이 되는 발행일. 여기서 날짜를 더하고 빼서 순서를 만든다. */
    private static final Instant BASE = Instant.parse("2026-11-01T00:00:00Z");

    private static final long DAY = 24 * 60 * 60;

    @Autowired
    WebApplicationContext context;

    @Autowired
    PostRepository postRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    EntityManager entityManager;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void 게시_글이_없으면_빈_목록을_내려준다() throws Exception {
        draft("draft-only");

        perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(0)))
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.nextCursor", nullValue()));
    }

    @Test
    void 글마다_제목_슬러그_요약_태그_발행일을_내려준다() throws Exception {
        Tag spring = tag("Spring");
        Tag jpa = tag("JPA");
        Post post = Post.draft("JPA N+1 문제", "jpa-n-plus-one", "조회가 글 수만큼 더 나갔어요.", "# 본문");
        post.addTag(spring);
        post.addTag(jpa);
        post.publish(Instant.parse("2026-11-18T00:00:00Z"));
        postRepository.save(post);

        perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(1)))
                .andExpect(jsonPath("$.posts[0].title").value("JPA N+1 문제"))
                .andExpect(jsonPath("$.posts[0].slug").value("jpa-n-plus-one"))
                .andExpect(jsonPath("$.posts[0].summary").value("조회가 글 수만큼 더 나갔어요."))
                // 태그는 이름순이다
                .andExpect(jsonPath("$.posts[0].tags", contains("JPA", "Spring")))
                .andExpect(jsonPath("$.posts[0].publishedAt").value("2026-11-18T00:00:00Z"))
                // 목록에는 본문을 싣지 않는다
                .andExpect(jsonPath("$.posts[0].content").doesNotExist());
    }

    @Test
    void 초안은_목록과_전체_글_수에서_빠진다() throws Exception {
        published("published", BASE);
        draft("draft");

        perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].slug", contains("published")))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void 발행일이_최신인_글부터_내려준다() throws Exception {
        // 저장한 순서와 발행일 순서를 일부러 다르게 둔다
        published("middle", BASE.plusSeconds(DAY));
        published("newest", BASE.plusSeconds(2 * DAY));
        published("oldest", BASE);

        perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].slug", contains("newest", "middle", "oldest")));
    }

    @Test
    void 한_번에_20개를_내려주고_다음_커서로_나머지를_이어_받는다() throws Exception {
        // post-01이 가장 오래된 글, post-25가 가장 새 글이다
        for (int i = 1; i <= 25; i++) {
            published("post-%02d".formatted(i), BASE.plusSeconds(i * DAY));
        }

        String first = perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(20)))
                .andExpect(jsonPath("$.posts[0].slug").value("post-25"))
                .andExpect(jsonPath("$.posts[19].slug").value("post-06"))
                .andExpect(jsonPath("$.totalCount").value(25))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String nextCursor = JsonPath.read(first, "$.nextCursor");

        perform(get("/api/posts").param("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].slug", contains("post-05", "post-04", "post-03", "post-02", "post-01")))
                // 전체 글 수는 어디까지 읽었는지와 상관없이 같다
                .andExpect(jsonPath("$.totalCount").value(25))
                .andExpect(jsonPath("$.nextCursor", nullValue()));
    }

    @Test
    void 글이_딱_20개면_다음_커서가_없다() throws Exception {
        for (int i = 1; i <= 20; i++) {
            published("post-%02d".formatted(i), BASE.plusSeconds(i * DAY));
        }

        perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(20)))
                .andExpect(jsonPath("$.nextCursor", nullValue()));
    }

    @Test
    void 발행일이_같은_글도_빠지거나_겹치지_않고_이어_받는다() throws Exception {
        for (int i = 1; i <= 5; i++) {
            published("same-time-" + i, BASE);
        }

        List<String> slugs = new ArrayList<>();
        String cursor = null;
        do {
            var request = get("/api/posts").param("size", "2");
            if (cursor != null) {
                request.param("cursor", cursor);
            }
            String body = perform(request)
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            slugs.addAll(JsonPath.<List<String>>read(body, "$.posts[*].slug"));
            cursor = JsonPath.read(body, "$.nextCursor");
        } while (cursor != null);

        // 발행일이 같으면 나중에 저장한 글이 먼저 나온다
        assertThat(slugs)
                .containsExactly("same-time-5", "same-time-4", "same-time-3", "same-time-2", "same-time-1");
    }

    @Test
    void size로_받을_개수를_정한다() throws Exception {
        for (int i = 1; i <= 7; i++) {
            published("post-" + i, BASE.plusSeconds(i * DAY));
        }

        perform(get("/api/posts").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].slug", contains("post-7", "post-6", "post-5", "post-4", "post-3")))
                .andExpect(jsonPath("$.totalCount").value(7))
                .andExpect(jsonPath("$.nextCursor").isString());
    }

    @Test
    void size가_1부터_20_사이가_아니면_400이다() throws Exception {
        perform(get("/api/posts").param("size", "0")).andExpect(status().isBadRequest());
        perform(get("/api/posts").param("size", "21")).andExpect(status().isBadRequest());
        perform(get("/api/posts").param("size", "many")).andExpect(status().isBadRequest());
    }

    @Test
    void 알_수_없는_커서는_400이다() throws Exception {
        perform(get("/api/posts").param("cursor", "not-a-cursor")).andExpect(status().isBadRequest());
    }

    @Test
    void 태그로_거르면_그_태그가_붙은_게시_글만_내려주고_전체_글_수도_그_기준이다() throws Exception {
        Tag spring = tag("Spring");
        Tag react = tag("React");
        published("spring-old", BASE, spring);
        published("react-only", BASE.plusSeconds(DAY), react);
        published("spring-and-react", BASE.plusSeconds(2 * DAY), spring, react);
        published("no-tag", BASE.plusSeconds(3 * DAY));

        perform(get("/api/posts").param("tag", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].slug", contains("spring-and-react", "spring-old")))
                // 거른 뒤에도 글에 붙은 태그는 모두 내려준다
                .andExpect(jsonPath("$.posts[0].tags", contains("React", "Spring")))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.nextCursor", nullValue()));
    }

    @Test
    void 태그로_걸러도_초안은_나오지_않는다() throws Exception {
        Tag spring = tag("Spring");
        published("spring-published", BASE, spring);
        Post draft = Post.draft("초안", "spring-draft", "요약", "본문");
        draft.addTag(spring);
        postRepository.save(draft);

        perform(get("/api/posts").param("tag", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].slug", contains("spring-published")))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void 태그로_거른_목록도_다음_커서로_이어_받는다() throws Exception {
        Tag spring = tag("Spring");
        for (int i = 1; i <= 3; i++) {
            published("spring-" + i, BASE.plusSeconds(2 * i * DAY), spring);
            published("other-" + i, BASE.plusSeconds((2 * i + 1) * DAY));
        }

        String first = perform(get("/api/posts").param("tag", "Spring").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].slug", contains("spring-3", "spring-2")))
                .andExpect(jsonPath("$.totalCount").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String nextCursor = JsonPath.read(first, "$.nextCursor");

        perform(get("/api/posts").param("tag", "Spring").param("size", "2").param("cursor", nextCursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[*].slug", contains("spring-1")))
                .andExpect(jsonPath("$.nextCursor", nullValue()));
    }

    @Test
    void 없는_태그로_거르면_빈_목록이다() throws Exception {
        published("post", BASE, tag("Spring"));

        perform(get("/api/posts").param("tag", "없는태그"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(0)))
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    void 태그_값이_비어_있으면_거르지_않는다() throws Exception {
        published("tagged", BASE, tag("Spring"));
        published("no-tag", BASE.plusSeconds(DAY));

        perform(get("/api/posts").param("tag", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(2)))
                .andExpect(jsonPath("$.totalCount").value(2));
    }

    /**
     * API를 부른다. 부르기 전에 메모리에 남은 엔티티를 비운다.
     * 테스트와 API가 같은 트랜잭션을 쓰기 때문에, 비우지 않으면 API가 DB를 읽지 않고
     * 테스트가 방금 만든 객체를 그대로 돌려받는다.
     */
    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        entityManager.flush();
        entityManager.clear();
        return mockMvc.perform(request);
    }

    private Tag tag(String name) {
        return tagRepository.save(new Tag(name));
    }

    private Post published(String slug, Instant publishedAt, Tag... tags) {
        Post post = Post.draft(slug + " 제목", slug, slug + " 요약", "본문");
        for (Tag tag : tags) {
            post.addTag(tag);
        }
        post.publish(publishedAt);
        return postRepository.save(post);
    }

    private Post draft(String slug) {
        return postRepository.save(Post.draft(slug + " 제목", slug, slug + " 요약", "본문"));
    }
}
