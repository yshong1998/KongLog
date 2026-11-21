package my.konglog.post;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;

/**
 * "더 보기"에서 어디까지 읽었는지 가리키는 값. 마지막으로 내려준 글의 발행일과 id다.
 *
 * <p>페이지 번호 대신 이 값을 쓰면, 목록을 읽는 사이에 새 글이 게시돼도 글이 밀리거나 겹치지 않는다.
 * 발행일이 같은 글이 있을 수 있어서 id를 함께 쓴다.
 */
public record PostCursor(Instant publishedAt, long id) {

    public static PostCursor of(Post post) {
        return new PostCursor(post.getPublishedAt(), post.getId());
    }

    /** API로 주고받는 글자로 바꾼다. 호출하는 쪽은 뜻을 몰라도 되고, 받은 그대로 돌려주면 된다. */
    public String encode() {
        String raw = publishedAt + "," + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@link #encode()}로 만든 글자를 다시 읽는다.
     *
     * @throws IllegalArgumentException 이 API가 만든 값이 아닐 때
     */
    public static PostCursor decode(String encoded) {
        try {
            // Base64가 아닌 글자가 섞여 있으면 여기서 IllegalArgumentException이 난다.
            String raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            int comma = raw.lastIndexOf(',');
            if (comma < 0) {
                throw new IllegalArgumentException("커서 형식이 올바르지 않다: " + encoded);
            }
            Instant publishedAt = Instant.parse(raw.substring(0, comma));
            long id = Long.parseLong(raw.substring(comma + 1));
            return new PostCursor(publishedAt, id);
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new IllegalArgumentException("커서 형식이 올바르지 않다: " + encoded, e);
        }
    }
}
