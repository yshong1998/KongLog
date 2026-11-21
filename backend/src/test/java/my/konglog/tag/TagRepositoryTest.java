package my.konglog.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/** 태그가 실제 PostgreSQL에 저장되는지 확인한다. 테스트마다 트랜잭션을 되돌린다. */
@SpringBootTest
@Transactional
class TagRepositoryTest {

    @Autowired
    TagRepository tagRepository;

    @Test
    void 태그를_저장하고_다시_읽는다() {
        Tag saved = tagRepository.saveAndFlush(new Tag("test-tag"));

        Tag found = tagRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getName()).isEqualTo("test-tag");
    }

    @Test
    void 같은_이름의_태그는_저장할_수_없다() {
        tagRepository.saveAndFlush(new Tag("test-same-name"));

        assertThatThrownBy(() -> tagRepository.saveAndFlush(new Tag("test-same-name")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
