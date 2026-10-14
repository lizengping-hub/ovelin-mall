package com.ovelin.mall.user.api;

public interface UserQueryService {
    UserDTO findById(Long id);
    boolean isActive(Long userId);
}
