package my.konglog.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import my.konglog.tag.Tag;
import my.konglog.tag.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * 글이 실제 PostgreSQL에 저장되고 읽히는지 확인한다.
 * 테스트마다 트랜잭션을 되돌리므로 로컬 DB에 흔적이 남지 않는다.
 */
@SpringBootTest
@Transactional
class PostRepositoryTest {

    @Autowired
    PostRepository postRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    void 새_글은_초안으로_저장되고_만든_시각이_채워진다() {
        Post saved = postRepository.save(Post.draft("제목", "test-new-post", "요약", "# 본문"));
        flushAndClear();

        Post found = postRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTitle()).isEqualTo("제목");
        assertThat(found.getSlug()).isEqualTo("test-new-post");
        assertThat(found.getSummary()).isEqualTo("요약");
        assertThat(found.getBody()).isEqualTo("# 본문");
        assertThat(found.getStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(found.getPublishedAt()).isNull();
        assertThat(found.getEditedAt()).isNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void 게시하면_상태와_발행일이_저장된다() {
        Instant now = Instant.parse("2026-11-21T10:00:00Z");
        Post post = Post.draft("제목", "test-published-post", "요약", "본문");
        post.publish(now);
        postRepository.save(post);
        flushAndClear();

        Post found = postRepository.findById(post.getId()).orElseThrow();

        assertThat(found.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(found.getPublishedAt()).isEqualTo(now);
    }

    @Test
    void 이미_게시된_글을_다시_게시해도_발행일은_그대로다() {
        Instant first = Instant.parse("2026-11-21T10:00:00Z");
        Post post = Post.draft("제목", "test-publish-twice", "요약", "본문");

        post.publish(first);
        post.publish(first.plusSeconds(3600));

        assertThat(post.getPublishedAt()).isEqualTo(first);
    }

    @Test
    void 글에_단_태그를_함께_읽는다() {
        Tag spring = tagRepository.save(new Tag("test-spring"));
        Tag jpa = tagRepository.save(new Tag("test-jpa"));
        Post post = Post.draft("제목", "test-tagged-post", "요약", "본문");
        post.addTag(spring);
        post.addTag(jpa);
        postRepository.save(post);
        flushAndClear();

        Post found = postRepository.findById(post.getId()).orElseThrow();

        assertThat(found.getTags()).extracting(Tag::getName)
                .containsExactlyInAnyOrder("test-spring", "test-jpa");
    }

    @Test
    void 같은_슬러그로는_저장할_수_없다() {
        postRepository.saveAndFlush(Post.draft("첫 글", "test-same-slug", "요약", "본문"));

        assertThatThrownBy(() ->
                postRepository.saveAndFlush(Post.draft("둘째 글", "test-same-slug", "요약", "본문")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** DB에 실제로 쓰고, 메모리에 남은 객체를 비워서 다음 조회가 DB에서 읽게 한다. */
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
