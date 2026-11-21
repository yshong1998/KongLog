package my.konglog.tag;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import my.konglog.post.Post;
import my.konglog.post.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * GET /api/tags 를 실제 DB에 대고 확인한다.
 * 테스트마다 트랜잭션을 되돌리므로 DB에 데이터가 남지 않는다.
 */
@SpringBootTest
@Transactional
class TagListApiTest {

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
    void 게시_글이_없으면_빈_목록을_내려준다() throws Exception {
        draft("draft-only", tag("Spring"));

        getTags()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags", hasSize(0)));
    }

    @Test
    void 태그마다_이름과_게시_글_수를_내려준다() throws Exception {
        Tag spring = tag("Spring");
        published("spring-1", spring);
        published("spring-2", spring);

        getTags()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags", hasSize(1)))
                .andExpect(jsonPath("$.tags[0].name").value("Spring"))
                .andExpect(jsonPath("$.tags[0].postCount").value(2));
    }

    @Test
    void 글이_많은_태그부터_내려준다() throws Exception {
        // 태그를 만든 순서와 글 수의 순서를 일부러 다르게 둔다
        Tag docker = tag("Docker");
        Tag spring = tag("Spring");
        Tag react = tag("React");
        published("post-1", spring);
        published("post-2", spring, react);
        published("post-3", spring, react, docker);

        getTags()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[*].name", contains("Spring", "React", "Docker")))
                .andExpect(jsonPath("$.tags[*].postCount", contains(3, 2, 1)));
    }

    @Test
    void 글_수가_같으면_이름순이다() throws Exception {
        // 이름순과 반대로 만든다
        Tag spring = tag("Spring");
        Tag jpa = tag("JPA");
        Tag docker = tag("Docker");
        published("post-1", spring, jpa, docker);

        getTags()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[*].name", contains("Docker", "JPA", "Spring")));
    }

    @Test
    void 글_수에는_게시_글만_센다() throws Exception {
        Tag spring = tag("Spring");
        published("published", spring);
        draft("draft-1", spring);
        draft("draft-2", spring);

        getTags()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[0].name").value("Spring"))
                .andExpect(jsonPath("$.tags[0].postCount").value(1));
    }

    @Test
    void 초안에만_붙은_태그는_나오지_않는다() throws Exception {
        published("published", tag("Spring"));
        draft("draft", tag("Secret"));

        getTags()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[*].name", contains("Spring")));
    }

    @Test
    void 글이_하나도_없는_태그는_나오지_않는다() throws Exception {
        published("published", tag("Spring"));
        tag("Unused");

        getTags()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[*].name", contains("Spring")));
    }

    /**
     * API를 부른다. 부르기 전에 메모리에 남은 엔티티를 비운다.
     * 테스트와 API가 같은 트랜잭션을 쓰기 때문에, 비우지 않으면 API가 DB를 읽지 않고
     * 테스트가 방금 만든 객체를 그대로 돌려받는다.
     */
    private ResultActions getTags() throws Exception {
        entityManager.flush();
        entityManager.clear();
        return mockMvc.perform(get("/api/tags"));
    }

    private Tag tag(String name) {
        return tagRepository.save(new Tag(name));
    }

    private Post published(String slug, Tag... tags) {
        Post post = draft(slug, tags);
        post.publish(PUBLISHED_AT);
        return post;
    }

    private Post draft(String slug, Tag... tags) {
        Post post = Post.draft(slug + " 제목", slug, slug + " 요약", "본문");
        for (Tag tag : tags) {
            post.addTag(tag);
        }
        return postRepository.save(post);
    }
}
