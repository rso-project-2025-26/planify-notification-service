package com.planify.notification;

import com.planify.notification.service.EmailService;
import com.planify.notification.service.SmsService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import static org.mockito.Mockito.mock;

@SpringBootTest
@ComponentScan(
        basePackages = "com.planify.notification",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {EmailService.class, SmsService.class}
        )
)
class NotificationServiceApplicationTests {

    @Configuration
    static class TestConfig {
        @Bean
        public EmailService emailService() {
            return mock(EmailService.class);
        }

        @Bean
        public SmsService smsService() {
            return mock(SmsService.class);
        }
    }

    @Test
    void contextLoads() {
    }
}
