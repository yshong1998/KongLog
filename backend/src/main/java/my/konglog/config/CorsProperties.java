package my.konglog.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS 설정값. CORS는 브라우저가 다른 주소의 API를 부를 수 있게 서버가 허락해 주는 설정이다.
 *
 * @param allowedOrigins 브라우저에서 API를 부를 수 있는 주소들(예: {@code http://localhost:3000}).
 *     비어 있으면 어떤 주소도 허용하지 않는다
 */
@ConfigurationProperties("konglog.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        // 값이 비어 있으면 "아무 주소도 허용하지 않음"이다. 빈 글자는 주소로 치지 않는다.
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream().map(String::strip).filter(origin -> !origin.isEmpty()).toList();
    }
}
