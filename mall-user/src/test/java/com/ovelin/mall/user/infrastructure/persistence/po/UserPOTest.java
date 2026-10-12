package com.ovelin.mall.user.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** 守卫测试：防止有人改掉注解后，注销脱敏或时间字段的行为悄悄失效。 */
@DisplayName("UserPO 字段策略")
class UserPOTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"avatarUrl", "birthYear", "birthMonth", "birthDay", "deregisteredAt"})
    @DisplayName("可空字段 updateStrategy 必须是 ALWAYS，否则 updateById 不会把 null 写入数据库")
    void nullableColumnsUpdateAlways(String field) throws NoSuchFieldException {
        TableField annotation = UserPO.class.getDeclaredField(field).getAnnotation(TableField.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.updateStrategy()).isEqualTo(FieldStrategy.ALWAYS);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"createdAt", "updatedAt"})
    @DisplayName("createdAt / updatedAt 必须只读")
    void timestampsAreReadOnly(String field) throws NoSuchFieldException {
        TableField annotation = UserPO.class.getDeclaredField(field).getAnnotation(TableField.class);

        assertThat(annotation.insertStrategy()).isEqualTo(FieldStrategy.NEVER);
        assertThat(annotation.updateStrategy()).isEqualTo(FieldStrategy.NEVER);
    }
}