package com.alpeerkaraca.tripservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

@SpringBootTest()
@ComponentScan(basePackages = {"com.alpeerkaraca.tripservice", "com.alpeerkaraca.common"})
class TripServiceApplicationTests extends AbstractIntegrationTest {
    @Test
    void contextLoads() {
    }

}
