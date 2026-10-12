package com.ovelin.mall.user.domain.model.valueobject;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.domain.model.enums.Gender;
import com.ovelin.mall.user.domain.model.enums.RegisterSource;
import com.ovelin.mall.user.domain.model.enums.UserStatus;

import java.time.Instant;


/** 持久化状态的领域层快照，仅用于 User.reconstitute。 */
public record UserSnapshot(
        ShardedId id,
        DisplayName displayName,
        RegisterSource registerSource,
        UserStatus status,
        Avatar avatar,
        Gender gender,
        Birthday birthday,
        Instant createdAt,
        Instant updatedAt,
        Instant deregisteredAt) {
}