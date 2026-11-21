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
 * 글이 테이블에 저장되고 다시 읽히는지 확인한다.
 * 테스트마다 트랜잭션을 되돌리므로 DB에 데이터가 남지 않는다.
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
    void 새_글은_초안으로_저장되고_발행일이_없다() {
        Post saved = postRepository.save(Post.draft("제목", "first-post", "요약", "# 본문"));
        flushAndClear();

        Post found = postRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTitle()).isEqualTo("제목");
        assertThat(found.getSlug()).isEqualTo("first-post");
        assertThat(found.getSummary()).isEqualTo("요약");
        assertThat(found.getContent()).isEqualTo("# 본문");
        assertThat(found.getStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(found.getPublishedAt()).isNull();
        assertThat(found.getEditedAt()).isNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isEqualTo(found.getCreatedAt());
    }

    @Test
    void 게시하면_상태와_발행일이_저장된다() {
        Instant publishedAt = Instant.parse("2026-11-21T09:00:00Z");
        Post post = Post.draft("제목", "published-post", "요약", "본문");
        post.publish(publishedAt);
        postRepository.save(post);
        flushAndClear();

        Post found = postRepository.findById(post.getId()).orElseThrow();

        assertThat(found.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(found.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    void 글에_붙인_태그가_함께_저장된다() {
        Tag spring = tagRepository.save(new Tag("Spring"));
        Tag jpa = tagRepository.save(new Tag("JPA"));
        Post post = Post.draft("제목", "tagged-post", "요약", "본문");
        post.addTag(spring);
        post.addTag(jpa);
        postRepository.save(post);
        flushAndClear();

        Post found = postRepository.findById(post.getId()).orElseThrow();

        assertThat(found.getTags()).extracting(Tag::getName).containsExactlyInAnyOrder("Spring", "JPA");
    }

    @Test
    void 글을_고치면_마지막_수정_시각이_바뀐다() {
        Post post = postRepository.save(Post.draft("제목", "updated-post", "요약", "본문"));
        flushAndClear();
        Post found = postRepository.findById(post.getId()).orElseThrow();
        Instant before = found.getUpdatedAt();

        found.publish(Instant.parse("2026-11-21T09:00:00Z"));
        flushAndClear();

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updated.getUpdatedAt()).isAfter(before);
        assertThat(updated.getCreatedAt()).isEqualTo(found.getCreatedAt());
    }

    @Test
    void 같은_슬러그로는_저장할_수_없다() {
        postRepository.saveAndFlush(Post.draft("첫 글", "same-slug", "요약", "본문"));

        assertThatThrownBy(() -> postRepository.saveAndFlush(Post.draft("둘째 글", "same-slug", "요약", "본문")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /** 쌓아 둔 변경을 DB에 보내고 메모리의 엔티티를 비운다. 그래야 다음 조회가 DB에서 다시 읽는다. */
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
