package my.konglog.post;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Hibernate가 DB로 보내는 SQL을 모아 두는 테스트용 도구.
 *
 * <p>Hibernate가 이 클래스를 직접 만들기 때문에 스프링 빈으로 주입할 수 없다. 그래서 기록을 static으로 둔다.
 */
public class SqlRecorder implements StatementInspector {

    private static final List<String> RECORDED = new CopyOnWriteArrayList<>();

    @Override
    public String inspect(String sql) {
        RECORDED.add(sql);
        return sql;
    }

    static void clear() {
        RECORDED.clear();
    }

    static List<String> recorded() {
        return List.copyOf(RECORDED);
    }
}
