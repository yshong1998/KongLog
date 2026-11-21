package my.konglog.post;

import static org.hamcrest.Matchers.contains;
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

    private static final String CONTENT = """
            ## 문제

            컨테이너 안에서 `localhost`가 안 잡혔어요.

            ```bash
            curl localhost:8080
            ```
            """;

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
    void 게시_글을_슬러그로_읽는다() throws Exception {
        Post post = Post.draft(
                "Docker 컨테이너에서 localhost가 안 잡혔던 이유",
                "docker-localhost",
                "컨테이너 안에서 localhost는 컨테이너 자신이었어요.",
                CONTENT);
        post.addTag(tagRepository.save(new Tag("네트워크")));
        post.addTag(tagRepository.save(new Tag("Docker")));
        post.publish(Instant.parse("2026-11-18T00:00:00Z"));
        postRepository.save(post);

        perform(get("/api/posts/docker-localhost"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Docker 컨테이너에서 localhost가 안 잡혔던 이유"))
                .andExpect(jsonPath("$.slug").value("docker-localhost"))
                .andExpect(jsonPath("$.summary").value("컨테이너 안에서 localhost는 컨테이너 자신이었어요."))
                // 본문은 마크다운 원문 그대로다
                .andExpect(jsonPath("$.content").value(CONTENT))
                // 태그는 이름순이다
                .andExpect(jsonPath("$.tags", contains("Docker", "네트워크")))
                .andExpect(jsonPath("$.publishedAt").value("2026-11-18T00:00:00Z"))
                // 게시한 뒤 본문을 고친 적이 없으면 수정일이 없다
                .andExpect(jsonPath("$.editedAt", nullValue()));
    }

    @Test
    void 게시_후_본문을_고친_글은_수정일을_함께_내려준다() throws Exception {
        Post post = Post.draft("제목", "edited-post", "요약", "본문");
        post.publish(Instant.parse("2026-11-18T00:00:00Z"));
        postRepository.save(post);
        // 본문을 고치는 기능은 아직 없어서(3단계), 수정일을 테이블에 직접 넣는다
        entityManager.flush();
        entityManager
                .createNativeQuery("update post set edited_at = :editedAt where id = :id")
                .setParameter("editedAt", Instant.parse("2026-11-20T03:30:00Z"))
                .setParameter("id", post.getId())
                .executeUpdate();

        perform(get("/api/posts/edited-post"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editedAt").value("2026-11-20T03:30:00Z"));
    }

    @Test
    void 없는_글은_404다() throws Exception {
        perform(get("/api/posts/no-such-post")).andExpect(status().isNotFound());
    }

    @Test
    void 초안은_없는_글과_똑같이_404다() throws Exception {
        postRepository.save(Post.draft("초안", "draft-post", "요약", "본문"));

        perform(get("/api/posts/draft-post")).andExpect(status().isNotFound());
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
}
