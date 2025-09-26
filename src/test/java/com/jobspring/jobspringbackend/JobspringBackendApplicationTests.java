package com.jobspring.jobspringbackend;

import com.jobspring.jobspringbackend.service.MailService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = "app.mail.enabled=false")
class JobspringBackendApplicationTests {
    @MockBean
    private MailService mailService;

    @Test
    void contextLoads() {
    }

}
