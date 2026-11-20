package my.konglog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 애플리케이션이 실제 PostgreSQL에 붙어서 뜨는지 확인한다.
 * 로컬에서는 infra/local/compose.yaml의 DB가 떠 있어야 통과한다.
 */
@SpringBootTest
class KongLogApplicationTests {

    @Test
    void 애플리케이션이_뜬다() {
    }
}
