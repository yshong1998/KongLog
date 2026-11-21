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

    private static final Instant PUBLISHED_AT = Instant.parse("2026-11-01T00:00:00Z");

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
    void 게시_글이_많은_태그부터_글_수와_함께_내려준다() throws Exception {
        Tag spring = tag("Spring");
        Tag react = tag("React");
        Tag docker = tag("Docker");
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
        Tag spring = tag("Spring");
        Tag docker = tag("Docker");
        Tag react = tag("React");
        published("post-1", spring, docker, react);

        getTags().andExpect(jsonPath("$.tags[*].name", contains("Docker", "React", "Spring")));
    }

    @Test
    void 글_수에는_게시_글만_센다() throws Exception {
        Tag spring = tag("Spring");
        published("published", spring);
        draft("draft-1", spring);
        draft("draft-2", spring);

        getTags()
                .andExpect(jsonPath("$.tags", hasSize(1)))
                .andExpect(jsonPath("$.tags[0].name").value("Spring"))
                .andExpect(jsonPath("$.tags[0].postCount").value(1));
    }

    @Test
    void 초안에만_붙은_태그와_쓰지_않는_태그는_나오지_않는다() throws Exception {
        published("published", tag("Spring"));
        draft("draft", tag("초안에만"));
        tag("안쓰는태그");

        getTags().andExpect(jsonPath("$.tags[*].name", contains("Spring")));
    }

    @Test
    void 게시_글이_없으면_빈_목록을_내려준다() throws Exception {
        draft("draft", tag("Spring"));

        getTags().andExpect(status().isOk()).andExpect(jsonPath("$.tags", hasSize(0)));
    }

    /** API를 부른다. 부르기 전에 메모리에 남은 엔티티를 비워서 API가 DB에서 새로 읽게 한다. */
    private ResultActions getTags() throws Exception {
        entityManager.flush();
        entityManager.clear();
        return mockMvc.perform(get("/api/tags"));
    }

    private Tag tag(String name) {
        return tagRepository.save(new Tag(name));
    }

    private void published(String slug, Tag... tags) {
        Post post = newPost(slug, tags);
        post.publish(PUBLISHED_AT);
        postRepository.save(post);
    }

    private void draft(String slug, Tag... tags) {
        postRepository.save(newPost(slug, tags));
    }

    private Post newPost(String slug, Tag... tags) {
        Post post = Post.draft(slug + " 제목", slug, slug + " 요약", "본문");
        for (Tag tag : tags) {
            post.addTag(tag);
        }
        return post;
    }
}
