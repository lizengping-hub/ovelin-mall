package com.ovelin.mall.user.domain.model.aggregate;


import com.ovelin.mall.common.sharding.core.ddd.ShardedAggregateRoot;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.user.domain.exception.InvalidUserStatusException;
import com.ovelin.mall.user.domain.model.enums.Gender;
import com.ovelin.mall.user.domain.model.enums.RegisterSource;
import com.ovelin.mall.user.domain.model.enums.UserStatus;
import com.ovelin.mall.user.domain.model.valueobject.Avatar;
import com.ovelin.mall.user.domain.model.valueobject.Birthday;
import com.ovelin.mall.user.domain.model.valueobject.DisplayName;
import com.ovelin.mall.user.domain.model.valueobject.UserSnapshot;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
/**
 * 用户资料聚合。联系方式（手机/邮箱）属于认证模块的 UserIdentity，不在这里。
 * createdAt / updatedAt 由数据库维护：新建时为 null，持久化后读取才有值。
 * deregisteredAt 是业务时间：仅注销时由应用服务传入，未注销为 null。
 */
@Getter
public class User implements ShardedAggregateRoot<ShardedId> {

    private final ShardedId id;
    private final RegisterSource registerSource;
    private DisplayName displayName;
    private Avatar avatar;
    private Gender gender;
    private Birthday birthday;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deregisteredAt;

    private User(ShardedId id, DisplayName displayName, RegisterSource registerSource, UserStatus status) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
        this.registerSource = Objects.requireNonNull(registerSource, "registerSource must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    /** 业务入口：新注册用户。 */
    public static User register(ShardedId id, DisplayName displayName, RegisterSource registerSource) {
        User user = new User(id, displayName, registerSource, UserStatus.ACTIVE);
        user.gender = Gender.UnKnown;
        return user;
    }

    /** 仅供 Repository / 持久化转换使用：按存储状态还原，不校验业务规则。 */
    public static User reconstitute(UserSnapshot s) {
        User user = new User(s.id(), s.displayName(), s.registerSource(), s.status());
        user.avatar = s.avatar();
        user.gender = s.gender() != null ? s.gender() : Gender.UnKnown;
        user.birthday = s.birthday();
        user.createdAt = s.createdAt();
        user.updatedAt = s.updatedAt();
        user.deregisteredAt = s.deregisteredAt();
        return user;
    }

    public void ban() {
        requireActive("ban");
        this.status = UserStatus.DISABLED;
    }

    public void unban() {
        if (this.status != UserStatus.DISABLED) {
            throw new InvalidUserStatusException(id.value(), status, "unban");
        }
        this.status = UserStatus.ACTIVE;
    }

    public void changeDisplayName(DisplayName newDisplayName) {
        requireActive("changeDisplayName");
        this.displayName = Objects.requireNonNull(newDisplayName, "displayName must not be null");
    }

    /** avatar 允许为 null，表示清除头像。 */
    public void changeAvatar(Avatar newAvatar) {
        requireActive("changeAvatar");
        this.avatar = newAvatar;
    }

    public void changeGender(Gender newGender) {
        requireActive("changeGender");
        this.gender = Objects.requireNonNull(newGender, "gender must not be null");
    }

    /** birthday 允许为 null，表示清除生日。 */
    public void changeBirthday(Birthday newBirthday) {
        requireActive("changeBirthday");
        this.birthday = newBirthday;
    }

    /**
     * 注销：ACTIVE / DISABLED -> DELETED（终态），并清除全部个人信息，只留不可识别的"墓碑"。
     * 其他模块（认证、通知、订单…）的清理由应用服务发布 UserDeregistered 事件驱动。
     *
     * @param at 注销时间，由应用服务从统一 Clock 取得，领域层不自己取时间
     */
    public void deregister(Instant at) {
        Objects.requireNonNull(at, "deregisteredAt must not be null");
        if (this.status == UserStatus.DELETED) {
            throw new InvalidUserStatusException(id.value(), status, "deregister");
        }
        this.status = UserStatus.DELETED;
        this.deregisteredAt = at;
        this.displayName = DisplayName.deregistered();
        this.avatar = null;
        this.birthday = null;
        this.gender = Gender.UnKnown;
    }

    public boolean isDeregistered() {
        return status == UserStatus.DELETED;
    }

    private void requireActive(String operation) {
        if (this.status != UserStatus.ACTIVE) {
            throw new InvalidUserStatusException(id.value(), status, operation);
        }
    }
}