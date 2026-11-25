package my.konglog.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PostCursorTest {

    @Test
    void 글자로_바꿨다가_다시_읽으면_같은_값이다() {
        PostCursor cursor = new PostCursor(Instant.parse("2026-11-21T09:00:00.123456Z"), 42);

        assertThat(PostCursor.decode(cursor.encode())).isEqualTo(cursor);
    }

    @Test
    void 주소에_그대로_넣을_수_있는_글자만_쓴다() {
        PostCursor cursor = new PostCursor(Instant.parse("2026-11-21T09:00:00.123456Z"), 42);

        assertThat(cursor.encode()).matches("[A-Za-z0-9_-]+");
    }

    @ParameterizedTest
    // 차례로: Base64가 아닌 글자, "hello", "2026-11-21T09:00:00Z"(id 없음), "어제,1", "2026-11-21T09:00:00Z,abc"
    @ValueSource(strings = {"!!!", "aGVsbG8", "MjAyNi0xMS0yMVQwOTowMDowMFo", "7Ja07KCcLDE", "MjAyNi0xMS0yMVQwOTowMDowMFosYWJj"})
    void 이_API가_만들지_않은_값은_읽지_못한다(String encoded) {
        assertThatThrownBy(() -> PostCursor.decode(encoded)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"+10000-01-01T00:00:00Z", "-0001-12-31T23:59:59Z", "+999999999-12-31T23:59:59Z"})
    void 발행일이_1년부터_9999년_사이가_아니면_읽지_못한다(String publishedAt) {
        String encoded = new PostCursor(Instant.parse(publishedAt), 1).encode();

        assertThatThrownBy(() -> PostCursor.decode(encoded)).isInstanceOf(IllegalArgumentException.class);
    }
}
