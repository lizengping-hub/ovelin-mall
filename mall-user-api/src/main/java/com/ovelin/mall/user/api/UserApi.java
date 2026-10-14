package com.ovelin.mall.user.api;

import java.time.Instant;

public interface UserApi {
    void createUser(CreateUserCommand command);
    void createUser(Long userId, String displayName, String registerSource);
    void deregister(Long id, Instant at);

}