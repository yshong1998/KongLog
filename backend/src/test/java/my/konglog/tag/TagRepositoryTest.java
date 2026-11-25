package my.konglog.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/** 태그가 테이블에 저장되고 다시 읽히는지 확인한다. */
@SpringBootTest
@Transactional
class TagRepositoryTest {

    @Autowired
    TagRepository tagRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    void 태그를_저장하고_다시_읽는다() {
        Tag saved = tagRepository.save(new Tag("Spring"));
        entityManager.flush();
        entityManager.clear();

        Tag found = tagRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getName()).isEqualTo("Spring");
    }

    @Test
    void 같은_이름의_태그는_저장할_수_없다() {
        tagRepository.saveAndFlush(new Tag("Spring"));

        assertThatThrownBy(() -> tagRepository.saveAndFlush(new Tag("Spring")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"spring", "SPRING", "sPrInG"})
    void 대소문자만_다른_이름의_태그는_저장할_수_없다(String name) {
        tagRepository.saveAndFlush(new Tag("Spring"));

        assertThatThrownBy(() -> tagRepository.saveAndFlush(new Tag(name)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 띄어쓰기만_다른_이름의_태그는_따로_저장된다() {
        tagRepository.saveAndFlush(new Tag("Spring Boot"));
        tagRepository.saveAndFlush(new Tag("SpringBoot"));

        assertThat(tagRepository.count()).isEqualTo(2);
    }

    @Test
    void 다듬은_이름으로_저장된다() {
        Tag saved = tagRepository.save(new Tag("  Spring   Boot "));
        entityManager.flush();
        entityManager.clear();

        assertThat(tagRepository.findById(saved.getId()).orElseThrow().getName()).isEqualTo("Spring Boot");
    }

    @Test
    void 이모지가_든_50자_이름도_저장된다() {
        // 자바의 length()로는 52지만 DB의 varchar(50)은 글자 수로 센다
        String name = "😀".repeat(2) + "a".repeat(48);

        Tag saved = tagRepository.saveAndFlush(new Tag(name));

        assertThat(saved.getName()).isEqualTo(name);
    }
}
