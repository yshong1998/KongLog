package my.konglog.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import my.konglog.tag.Tag;
import my.konglog.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * GET /api/posts 가 DB에 보내는 SQL을 확인한다. 응답 내용은 {@link PostListApiTest}에서 본다.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector=my.konglog.post.SqlRecorder")
@Transactional
class PostListQueryTest {

    private static final Instant BASE = Instant.parse("2026-11-01T00:00:00Z");

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
    void 글_목록은_본문을_읽지_않는다() throws Exception {
        savePublishedPosts(5);

        List<String> sqls = callList("/api/posts");

        assertThat(sqls).isNotEmpty().noneMatch(sql -> sql.contains("content"));
    }

    @Test
    void 글이_5개일_때와_20개일_때_조회_수가_같다() throws Exception {
        savePublishedPosts(20);

        int forFive = callList("/api/posts?size=5").size();
        int forTwenty = callList("/api/posts?size=20").size();

        assertThat(forFive).isEqualTo(3);
        assertThat(forTwenty).isEqualTo(forFive);
    }

    /** 글을 만들고 메모리를 비운 뒤 API를 불러, 그 요청에서 나간 SQL만 돌려준다. */
    private List<String> callList(String url) throws Exception {
        entityManager.flush();
        entityManager.clear();
        SqlRecorder.clear();
        mockMvc.perform(get(url)).andExpect(status().isOk());
        return SqlRecorder.recorded();
    }

    private void savePublishedPosts(int count) {
        Tag spring = tagRepository.save(new Tag("Spring"));
        Tag jpa = tagRepository.save(new Tag("JPA"));
        for (int i = 0; i < count; i++) {
            Post post = Post.draft("제목 " + i, "post-" + i, "요약 " + i, "본문 " + i);
            post.addTag(spring);
            post.addTag(jpa);
            post.publish(BASE.plusSeconds(i));
            postRepository.save(post);
        }
    }
}
