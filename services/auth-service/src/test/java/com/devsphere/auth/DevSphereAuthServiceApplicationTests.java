package com.devsphere.auth;

import com.devsphere.auth.event.UserRegisteredEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DevSphereAuthServiceApplicationTests {

    @MockBean
    private KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    @Test
    void contextLoads() {
    }
}
