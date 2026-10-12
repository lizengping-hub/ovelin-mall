package com.ovelin.mall.common.sharding.core.ov;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class IdLayoutTest {
    @Test
    void shouldRejectInvalidSequence() {
        assertThrows(IllegalArgumentException.class, () -> IdLayout.validateSequence(-1L));
        // 左移 SHARD_BITS 后会溢出,必须被拒绝而不是悄悄产生错误的 id
        assertThrows(IllegalArgumentException.class, () -> IdLayout.validateSequence(Long.MAX_VALUE));
        // 左移 SHARD_BITS 后会溢出,必须被拒绝而不是悄悄产生错误的 id
        assertThrows(IllegalArgumentException.class, () -> IdLayout.validateSequence(IdLayout.MAX_SEQUENCE + 1));
    }
}
