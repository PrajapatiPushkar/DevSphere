package com.devsphere.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.eureka.server.EurekaServerConfigBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DevSphereServiceDiscoveryApplicationTest {

    @Autowired(required = false)
    private EurekaServerConfigBean eurekaServerConfigBean;

    @Test
    void contextLoads_andEurekaServerConfigured() {
        assertThat(eurekaServerConfigBean).isNotNull();
    }
}
