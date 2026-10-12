package com.ovelin.mall.user.application.service;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.domain.exception.UserNotFoundException;
import com.ovelin.mall.user.domain.model.aggregate.User;

import com.ovelin.mall.user.domain.model.valueobject.DisplayName;

import com.ovelin.mall.user.domain.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserApplicationServiceTest {
    @Mock
    UserRepository userRepository;
    @InjectMocks
    UserApplicationService userApplicationService;

    @Test
    void updateDisplayName_shouldThrowWhenUserNotFound() {
        Long id = 123L;

        when(userRepository.findById(ShardedId.fromId(id)))
                .thenReturn(null);

        assertThrows(
                UserNotFoundException.class,
                () -> userApplicationService.updateDisplayName(id, "Alice")
        );

        verify(userRepository, never()).update(any());
    }
    @Test
    void updateDisplayName_shouldUpdateUser() {
        Long id = 123L;
        User user = mock(User.class);

        when(userRepository.findById(ShardedId.fromId(id)))
                .thenReturn(user);

        userApplicationService.updateDisplayName(id, "Alice");

        verify(user).changeDisplayName(DisplayName.from("Alice"));
        verify(userRepository).update(user);
    }
}
