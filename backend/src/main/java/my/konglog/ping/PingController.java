package my.konglog.ping;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 프론트엔드가 백엔드와 통신되는지 확인할 때 부르는 API. */
@RestController
@RequestMapping("/api")
public class PingController {

    static final String SERVICE_NAME = "konglog-backend";

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("ok", SERVICE_NAME);
    }
}
