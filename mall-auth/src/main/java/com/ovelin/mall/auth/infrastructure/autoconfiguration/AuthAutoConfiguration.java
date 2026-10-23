package com.ovelin.mall.auth.infrastructure.autoconfiguration;

import com.ovelin.mall.auth.application.port.PasswordPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class AuthAutoConfiguration {
    @Bean
    public PasswordEncoder passwordEncoder() {
        String idForEncode = "bcrypt";

        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", new BCryptPasswordEncoder(12));
        encoders.put("argon2", Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());

        return new DelegatingPasswordEncoder(idForEncode, encoders);
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
