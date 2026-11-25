package my.konglog.tag;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 글에 붙이는 태그.
 *
 * <p>이름은 다듬은 표기 그대로 저장한다. 대소문자만 다른 이름은 같은 태그라서 둘 생길 수 없다. 이 규칙은 DB의
 * 색인(uq_tag_name_lower)이 지킨다.
 */
@Entity
@Table(name = "tag")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA가 쓰는 기본 생성자
public class Tag {

    /** 이름의 최대 길이. DB의 varchar(50)과 같게 글자 수(코드 포인트)로 센다. */
    static final int MAX_NAME_LENGTH = 50;

    /** 공백 글자(일반 공백, 줄바꿈 없는 공백, 전각 공백 등)가 하나 이상 이어진 것. */
    private static final Pattern SPACES = Pattern.compile("\\p{Zs}+");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /**
     * 이름을 다듬어 태그를 만든다. 앞뒤 공백을 지우고, 사이의 연속 공백은 공백 하나로 줄인다.
     *
     * @throws IllegalArgumentException 다듬은 이름이 비었거나 50자를 넘을 때, 쉼표나 제어 문자(줄바꿈, 탭 등)가 있을 때
     */
    public Tag(String name) {
        this.name = normalizeName(name);
    }

    static String normalizeName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("태그 이름이 없다");
        }
        // 줄바꿈은 공백으로 줄이기 전에 막는다. 줄이고 나면 줄바꿈이 있었는지 알 수 없다.
        // 줄바꿈 말고 다른 제어 문자(탭, null 글자 등)도 막는다. 글 목록 API가 tag에 제어 문자가 있으면 400을
        // 내서, 이런 이름의 태그는 주소로 거를 수 없다.
        if (name.chars().anyMatch(c -> Character.isISOControl(c) || c == ' ' || c == ' ')) {
            throw new IllegalArgumentException("태그 이름에 줄바꿈이나 제어 문자를 쓸 수 없다");
        }
        if (name.indexOf(',') >= 0) {
            throw new IllegalArgumentException("태그 이름에 쉼표를 쓸 수 없다");
        }
        String normalized = SPACES.matcher(name).replaceAll(" ").strip();
        int length = normalized.codePointCount(0, normalized.length());
        if (length < 1 || length > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("태그 이름은 1자 이상 " + MAX_NAME_LENGTH + "자 이하다");
        }
        return normalized;
    }
}
