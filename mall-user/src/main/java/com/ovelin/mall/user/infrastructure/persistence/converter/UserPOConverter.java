package com.ovelin.mall.user.infrastructure.persistence.converter;

import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.domain.model.aggregate.User;
import com.ovelin.mall.user.domain.model.enums.Gender;
import com.ovelin.mall.user.domain.model.enums.RegisterSource;
import com.ovelin.mall.user.domain.model.enums.UserStatus;
import com.ovelin.mall.user.domain.model.valueobject.*;
import com.ovelin.mall.user.infrastructure.persistence.po.UserPO;



public final class UserPOConverter {

    private UserPOConverter() {}



    /** PO -> 聚合（还原，不走业务校验）。 */
    public static User toUser(UserPO po) {
        if (po == null) return null;

        return User.reconstitute(new UserSnapshot(
                ShardedId.fromId(po.getId()),
                DisplayName.reconstitute(po.getDisplayName()),
                RegisterSource.fromCode(po.getRegisterSource()),
                UserStatus.fromValue(po.getStatus()),
                po.getAvatarUrl() != null ? Avatar.reconstitute(po.getAvatarUrl()) : null,
                po.getGender() != null ? Gender.fromCode(po.getGender()) : null,
                Birthday.reconstituteNullable(po.getBirthYear(), po.getBirthMonth(), po.getBirthDay()).orElseGet(()->null),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getDeregisteredAt()));
    }

    /** 聚合 -> PO。不写 createdAt / updatedAt。 */
    public static UserPO toPO(User user) {
        UserPO po = new UserPO();
        po.setId(user.getId().value());
        po.setDisplayName(user.getDisplayName().value());
        po.setRegisterSource(user.getRegisterSource().code());
        po.setStatus(user.getStatus().getValue());
        po.setDeregisteredAt(user.getDeregisteredAt());
        po.setGender(user.getGender().getCode());
        po.setAvatarUrl(user.getAvatar() != null ? user.getAvatar().getUrl() : null);
        if (user.getBirthday() != null) {
            po.setBirthYear(user.getBirthday().getYear());   // 可为 null
            po.setBirthMonth(user.getBirthday().getMonth());
            po.setBirthDay(user.getBirthday().getDay());
        } else {
            po.setBirthYear(null);
            po.setBirthMonth(null);
            po.setBirthDay(null);
        }
        return po;
    }
}