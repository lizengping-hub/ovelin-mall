package com.ovelin.mall.auth.infrastructure.persistence.autoconfiguration;

import com.ovelin.mall.auth.application.port.PasswordPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class AuthAutoConfiguration {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public Clock clock(){
        return Clock.systemUTC();
    }
    @Bean
    public PasswordPolicy passwordPolicy(){
        return new PasswordPolicy(5, Duration.ofDays(1));
    }
}
