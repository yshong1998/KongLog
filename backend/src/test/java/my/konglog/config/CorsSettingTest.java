package my.konglog.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** 허용할 주소를 설정값으로 여러 개 넣었을 때를 확인한다. */
@SpringBootTest(properties = "konglog.cors.allowed-origins=http://a.example, http://b.example")
class CorsSettingTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void 쉼표로_넣은_주소마다_허용한다() throws Exception {
        mockMvc.perform(get("/api/tags").header(HttpHeaders.ORIGIN, "http://a.example"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://a.example"));
        mockMvc.perform(get("/api/tags").header(HttpHeaders.ORIGIN, "http://b.example"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://b.example"));
    }

    @Test
    void 설정에_없는_주소는_허용하지_않는다() throws Exception {
        mockMvc.perform(get("/api/tags").header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
