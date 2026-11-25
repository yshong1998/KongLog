package my.konglog.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** 태그를 만들 때 이름을 다듬고 검사하는지 확인한다. DB 없이 돈다. */
class TagNameTest {

    @Test
    void 표기는_입력한_그대로_둔다() {
        assertThat(new Tag("GitHub Actions").getName()).isEqualTo("GitHub Actions");
        assertThat(new Tag("Next.js").getName()).isEqualTo("Next.js");
        assertThat(new Tag("spring").getName()).isEqualTo("spring");
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            quoteCharacter = '"',
            value = {
                "\" Spring  Boot \"|Spring Boot",
                "\"   Spring\"|Spring",
                "\"Spring   \"|Spring",
                "\"Spring   Boot\"|Spring Boot",
                "\"　스프링　부트　\"|스프링 부트",
            })
    void 앞뒤_공백을_지우고_사이의_연속_공백은_하나로_줄인다(String input, String expected) {
        assertThat(new Tag(input).getName()).isEqualTo(expected);
    }

    @Test
    void 띄어쓰기는_지우지_않는다() {
        assertThat(new Tag("Spring Boot").getName()).isEqualTo("Spring Boot");
        assertThat(new Tag("SpringBoot").getName()).isEqualTo("SpringBoot");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "　"})
    void 다듬은_뒤_비어_있으면_만들_수_없다(String input) {
        assertThatThrownBy(() -> new Tag(input)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 이름이_없으면_만들_수_없다() {
        assertThatThrownBy(() -> new Tag(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 다듬은_뒤_50자까지는_만들_수_있다() {
        String fifty = "a".repeat(50);

        assertThat(new Tag(fifty).getName()).isEqualTo(fifty);
        // 앞뒤 공백은 다듬은 뒤에 세므로 길이에 들어가지 않는다
        assertThat(new Tag("  " + fifty + "  ").getName()).isEqualTo(fifty);
    }

    @Test
    void 다듬은_뒤_50자를_넘으면_만들_수_없다() {
        assertThatThrownBy(() -> new Tag("a".repeat(51))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 길이는_자바의_char가_아니라_글자_수로_센다() {
        // 이모지 하나는 자바에서 char 2개지만 한 글자다
        String fiftyWithEmoji = "😀" + "a".repeat(49);
        String fiftyOneWithEmoji = "😀" + "a".repeat(50);

        assertThat(new Tag(fiftyWithEmoji).getName()).isEqualTo(fiftyWithEmoji);
        assertThatThrownBy(() -> new Tag(fiftyOneWithEmoji)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Spring\nBoot", "Spring\r\nBoot", "Spring\n", "\nSpring", "Spring Boot"})
    void 줄바꿈이_있으면_만들_수_없다(String input) {
        assertThatThrownBy(() -> new Tag(input)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Spring,Boot", "Spring, Boot", ",", "Spring,"})
    void 쉼표가_있으면_만들_수_없다(String input) {
        assertThatThrownBy(() -> new Tag(input)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Spring\tBoot", "Spring\u0000", "\u007fSpring"})
    void 줄바꿈이_아닌_제어_문자도_있으면_만들_수_없다(String input) {
        assertThatThrownBy(() -> new Tag(input)).isInstanceOf(IllegalArgumentException.class);
    }
}
