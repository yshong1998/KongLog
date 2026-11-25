package my.konglog.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** ./gradlew bootRun 으로 띄웠을 때처럼 로컬 프론트 주소를 허용하고 CORS를 확인한다. 그 값은 build.gradle이 넣는다. */
@SpringBootTest(properties = "konglog.cors.allowed-origins=" + CorsLocalTest.FRONT)
class CorsLocalTest {

    static final String FRONT = "http://localhost:3000";

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void 로컬_프론트_주소의_요청에는_허용_헤더가_붙는다() throws Exception {
        mockMvc.perform(get("/api/tags").header(HttpHeaders.ORIGIN, FRONT))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT));
    }

    @Test
    void 다른_주소의_요청에는_허용_헤더가_붙지_않는다() throws Exception {
        mockMvc.perform(get("/api/tags").header(HttpHeaders.ORIGIN, "http://example.com"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void 쿠키를_함께_보내는_요청은_허용하지_않는다() throws Exception {
        mockMvc.perform(get("/api/tags").header(HttpHeaders.ORIGIN, FRONT))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void GET이_아닌_요청은_허용하지_않는다() throws Exception {
        // 브라우저는 GET이 아닌 요청을 보내기 전에 OPTIONS로 먼저 물어본다(사전 요청).
        mockMvc.perform(options("/api/tags")
                        .header(HttpHeaders.ORIGIN, FRONT)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void GET_사전_요청은_허용한다() throws Exception {
        mockMvc.perform(options("/api/tags")
                        .header(HttpHeaders.ORIGIN, FRONT)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT));
    }
}
