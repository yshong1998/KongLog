package my.konglog.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
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
}
