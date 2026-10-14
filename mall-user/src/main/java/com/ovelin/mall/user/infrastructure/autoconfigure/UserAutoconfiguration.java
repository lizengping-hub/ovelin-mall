package com.ovelin.mall.user.infrastructure.autoconfigure;

import com.ovelin.mall.user.api.UserApi;
import com.ovelin.mall.user.api.UserQueryService;
import com.ovelin.mall.user.application.service.UserAppService;
import com.ovelin.mall.user.application.service.UserQueryAppService;
import com.ovelin.mall.user.domain.repository.UserRepository;
import com.ovelin.mall.user.infrastructure.persistence.mapper.UserUpdateMapper;
import com.ovelin.mall.user.infrastructure.persistence.mapper.UserQueryMapper;
import com.ovelin.mall.user.infrastructure.persistence.repository.UserRepositoryImpl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.ovelin.mall.user.infrastructure.persistence.mapper")
public class UserAutoconfiguration {
    @Bean
    @ConditionalOnMissingBean
    UserRepository userRepository(UserUpdateMapper userMapper) {
        return new UserRepositoryImpl(userMapper);
    }
    @Bean
    @ConditionalOnMissingBean
    UserQueryService userQueryService(UserQueryMapper userQueryMapper){
        return new UserQueryAppService(userQueryMapper);
    }
    @Bean
    @ConditionalOnMissingBean
    UserApi userApplicationService(UserRepository userRepository) {
        return new UserAppService(userRepository);
    }
}
