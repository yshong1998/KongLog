package my.konglog.ping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PingControllerTest {

    @Test
    void 상태와_서비스_이름을_돌려준다() {
        PingResponse response = new PingController().ping();

        assertThat(response.status()).isEqualTo("ok");
        assertThat(response.service()).isEqualTo("konglog-backend");
    }
}
