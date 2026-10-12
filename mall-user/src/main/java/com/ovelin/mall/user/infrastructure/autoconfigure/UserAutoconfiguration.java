package com.ovelin.mall.user.infrastructure.autoconfigure;

import com.ovelin.mall.user.api.UserApi;
import com.ovelin.mall.user.api.UserQueryService;
import com.ovelin.mall.user.application.api.UserApiImpl;
import com.ovelin.mall.user.application.service.UserApplicationService;
import com.ovelin.mall.user.application.service.UserQueryApplicationService;
import com.ovelin.mall.user.domain.repository.UserRepository;
import com.ovelin.mall.user.infrastructure.persistence.mapper.UserUpdateMapper;
import com.ovelin.mall.user.infrastructure.persistence.mapper.UserQueryMapper;
import com.ovelin.mall.user.infrastructure.persistence.repository.UserRepositoryImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserAutoconfiguration {
    @Bean
    @ConditionalOnMissingBean
    UserRepository userRepository(UserUpdateMapper userMapper) {
        return new UserRepositoryImpl(userMapper);
    }
    @Bean
    @ConditionalOnMissingBean
    UserQueryService userQueryService(UserQueryMapper userQueryMapper){
        return new UserQueryApplicationService(userQueryMapper);
    }
    @Bean
    @ConditionalOnMissingBean
    UserApplicationService userApplicationService(UserRepository userRepository) {
        return new UserApplicationService(userRepository);
    }
    @Bean
    @ConditionalOnMissingBean
    UserApi userApi(UserRepository userRepository) {
        return new UserApiImpl(userRepository);
    }

}
