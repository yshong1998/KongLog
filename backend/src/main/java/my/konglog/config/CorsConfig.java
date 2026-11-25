package my.konglog.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * /api 주소의 CORS 설정. 로컬 개발용이다.
 *
 * <p>배포 환경에서는 프론트와 백엔드가 같은 도메인이라 CORS가 필요 없다. 그래서 허용할 주소를 따로 넣지 않으면
 * 설정 자체를 걸지 않는다.
 *
 * <p>지금은 읽기(GET)만 허용하고 쿠키는 받지 않는다. 다른 방식과 쿠키는 로그인을 만들 때 다시 정한다.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (corsProperties.allowedOrigins().isEmpty()) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET")
                .allowCredentials(false);
    }
}
