package com.ovelin.mall.user.api;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateUserCommandTest {

    private static ShardedId anyShardedId() {
        return ShardedId.of(1, 1);
    }

    @Test
    @DisplayName("只传必填项时，可选字段均为 null")
    void builder_withRequiredOnly_optionalFieldsAreNull() {
        ShardedId id = ShardedId.of(1, 100);
        CreateUserCommand cmd = CreateUserCommand.builder(id, "Tom", "WEB").build();

        assertThat(cmd.getDisplayName()).isEqualTo("Tom");
        assertThat(cmd.getRegisterSource()).isEqualTo("WEB");
        assertThat(cmd.getShardedId()).isEqualTo(id);
        assertThat(cmd.getAvatarUrl()).isNull();
        assertThat(cmd.getGender()).isNull();
        assertThat(cmd.getBirthYear()).isNull();
        assertThat(cmd.getBirthMonth()).isNull();
        assertThat(cmd.getBirthDay()).isNull();
    }

    @Test
    @DisplayName("所有字段都设置后能完整取回")
    void builder_withAllFields_populatesCommand() {
        ShardedId id = anyShardedId();

        CreateUserCommand cmd = CreateUserCommand.builder(id, "Tom", "APP")
                .shardId(id)
                .avatarUrl("https://cdn.example.com/a.png")
                .gender(1)
                .birthYear(1998)
                .birthDay(7, 21)
                .build();

        assertThat(cmd.getShardedId()).isSameAs(id);
        assertThat(cmd.getDisplayName()).isEqualTo("Tom");
        assertThat(cmd.getRegisterSource()).isEqualTo("APP");
        assertThat(cmd.getAvatarUrl()).isEqualTo("https://cdn.example.com/a.png");
        assertThat(cmd.getGender()).isEqualTo(1);
        assertThat(cmd.getBirthYear()).isEqualTo(1998);
        assertThat(cmd.getBirthMonth()).isEqualTo(7);
        assertThat(cmd.getBirthDay()).isEqualTo(21);
    }

    @Test
    @DisplayName("birthDay(month, day) 同时设置月和日")
    void birthDay_setsMonthAndDay() {
        CreateUserCommand cmd = CreateUserCommand.builder(anyShardedId(), "Tom", "WEB")
                .birthDay(12, 3)
                .build();

        assertThat(cmd.getBirthMonth()).isEqualTo(12);
        assertThat(cmd.getBirthDay()).isEqualTo(3);
    }

    @Test
    @DisplayName("只有月日、没有年份：birthYear 保持 null（支持隐藏出生年）")
    void birthYear_isOptionalWhenMonthDaySet() {
        CreateUserCommand cmd = CreateUserCommand.builder(anyShardedId(),"Tom", "WEB")
                .birthDay(2, 14)
                .build();

        assertThat(cmd.getBirthYear()).isNull();
        assertThat(cmd.getBirthMonth()).isEqualTo(2);
        assertThat(cmd.getBirthDay()).isEqualTo(14);
    }

    @Test
    @DisplayName("gender=0（未知）是有效值，不会被当成未设置")
    void gender_zeroIsPreserved() {
        CreateUserCommand cmd = CreateUserCommand.builder(anyShardedId(), "Tom", "WEB")
                .gender(0)
                .build();

        assertThat(cmd.getGender()).isEqualTo(0);
    }

    @Test
    @DisplayName("同一个 Builder 先 build 后再修改，不影响已构建的对象")
    void build_snapshotIsIndependentOfLaterBuilderChanges() {
        CreateUserCommand.Builder b = CreateUserCommand.builder(anyShardedId(), "Tom", "WEB").gender(1);
        CreateUserCommand first = b.build();

        b.gender(2);
        CreateUserCommand second = b.build();

        assertThat(first.getGender()).isEqualTo(1);
        assertThat(second.getGender()).isEqualTo(2);
    }
}