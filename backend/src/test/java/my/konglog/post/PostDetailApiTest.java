package my.konglog.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import my.konglog.tag.Tag;
import my.konglog.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * GET /api/posts/{슬러그} 를 실제 DB에 대고 확인한다.
 * 테스트마다 트랜잭션을 되돌리므로 DB에 데이터가 남지 않는다.
 */
@SpringBootTest
@Transactional
class PostDetailApiTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-11-18T00:00:00Z");

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
    void 게시_글을_슬러그로_읽으면_제목_슬러그_요약_본문_태그_발행일을_내려준다() throws Exception {
        String content = "# 문제\n\n조회가 글 수만큼 더 나갔어요.\n\n```java\nList<Post> posts;\n```\n";
        Post post = Post.draft("JPA N+1 문제", "jpa-n-plus-one", "조회가 글 수만큼 더 나갔어요.", content);
        post.addTag(tagRepository.save(new Tag("Spring")));
        post.addTag(tagRepository.save(new Tag("JPA")));
        post.publish(PUBLISHED_AT);
        postRepository.save(post);

        perform(get("/api/posts/jpa-n-plus-one"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("JPA N+1 문제"))
                .andExpect(jsonPath("$.slug").value("jpa-n-plus-one"))
                .andExpect(jsonPath("$.summary").value("조회가 글 수만큼 더 나갔어요."))
                // 본문은 마크다운 원문 그대로다
                .andExpect(jsonPath("$.content").value(content))
                // 태그는 이름순이다
                .andExpect(jsonPath("$.tags", contains("JPA", "Spring")))
                .andExpect(jsonPath("$.publishedAt").value("2026-11-18T00:00:00Z"));
    }

    @Test
    void 게시한_뒤_고친_적이_없으면_수정일이_null이다() throws Exception {
        published("never-edited");

        perform(get("/api/posts/never-edited"))
                .andExpect(status().isOk())
                // 필드는 그대로 있고 값이 null이다
                .andExpect(jsonPath("$.editedAt", nullValue()));
    }

    @Test
    void 게시한_뒤_본문을_고친_글은_수정일을_내려준다() throws Exception {
        published("edited");
        // 글을 고치는 기능은 아직 없어서, 고친 시각을 DB에 직접 적는다.
        entityManager.flush();
        entityManager
                .createNativeQuery("update post set edited_at = :editedAt where slug = :slug")
                .setParameter("editedAt", Instant.parse("2026-11-20T03:30:00Z"))
                .setParameter("slug", "edited")
                .executeUpdate();

        perform(get("/api/posts/edited"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedAt").value("2026-11-18T00:00:00Z"))
                .andExpect(jsonPath("$.editedAt").value("2026-11-20T03:30:00Z"));
    }

    @Test
    void 태그가_없는_글은_태그가_빈_목록이다() throws Exception {
        published("no-tag");

        perform(get("/api/posts/no-tag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags", hasSize(0)));
    }

    @Test
    void 없는_글의_슬러그로_부르면_404다() throws Exception {
        published("exists");

        perform(get("/api/posts/does-not-exist")).andExpect(status().isNotFound());
    }

    @Test
    void 초안의_슬러그로_부르면_404다() throws Exception {
        postRepository.save(Post.draft("초안", "draft-post", "요약", "본문"));

        perform(get("/api/posts/draft-post")).andExpect(status().isNotFound());
    }

    @Test
    void 초안의_404는_없는_글의_404와_똑같다() throws Exception {
        postRepository.save(Post.draft("초안", "draft-post", "요약", "본문"));

        MockHttpServletResponse draft =
                perform(get("/api/posts/draft-post")).andReturn().getResponse();
        MockHttpServletResponse missing =
                perform(get("/api/posts/does-not-exist")).andReturn().getResponse();

        // 응답만 보고는 초안이 있는지 알 수 없어야 한다
        assertThat(draft.getStatus()).isEqualTo(404);
        assertThat(draft.getStatus()).isEqualTo(missing.getStatus());
        assertThat(draft.getErrorMessage()).isEqualTo(missing.getErrorMessage());
        assertThat(draft.getContentAsString()).isEqualTo(missing.getContentAsString());
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

    private Post published(String slug) {
        Post post = Post.draft(slug + " 제목", slug, slug + " 요약", "본문");
        post.publish(PUBLISHED_AT);
        return postRepository.save(post);
    }
}
