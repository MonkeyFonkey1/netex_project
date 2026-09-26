package com.netex.addressbook.auth.signup;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SignupTopicConfig {

    @Bean
    NewTopic userSignupsTopic() {
        return new NewTopic(SignupEventPublisher.TOPIC, 1, (short) 1);
    }
}
