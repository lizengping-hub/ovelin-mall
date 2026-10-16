package com.ovelin.mall.common.sharding.core.ov;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 位布局往返、分片和序列越界、以及 equals/hashCode/toString 等行为测试。
 */
class ShardedIdTest {

    @Test
    void shouldRoundTripShardAndSequence() {
        ShardedId id = ShardedId.of(5, 12345L);

        assertEquals(5, id.shardId().value());
        assertEquals(12345L, id.sequence());
        assertEquals(5, ShardedId.shardOf(id.value()));

        ShardedId parsed = ShardedId.fromId(id.value());
        assertEquals(id.value(), parsed.value());
        assertEquals(id.shardId(), parsed.shardId());
        assertEquals(id.sequence(), parsed.sequence());
    }

    @Test
    void intAndShardIdFactoriesShouldProduceSameValue() {
        assertEquals(
            ShardedId.of(ShardId.of(4), 77L).value(),
            ShardedId.of(4, 77L).value()
        );
    }

    @Test
    void shouldSupportMaxShard() {
        ShardId max = ShardId.of(IdLayout.MAX_SHARD);

        ShardedId id = ShardedId.of(max, 1L);

        assertEquals(max, id.shardId());
        assertEquals(1L, id.sequence());
    }


    @Test
    void shouldRejectNegativeRawId() {
        assertThrows(IllegalArgumentException.class, () -> ShardedId.fromId(-1L));
        assertThrows(IllegalArgumentException.class, () -> ShardedId.shardOf(-1L));
    }

    /**
     * 当前 ShardedId 只重写了 hashCode 没有重写 equals,这个用例预期会失败。
     * 修复:补上 equals(按 id 比较),或把 ShardedId 改成 record。
     */
    @Test
    void sameIdShouldBeEqual() {
        ShardedId a = ShardedId.fromId(1L);
        ShardedId b = ShardedId.fromId(1L);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toStringShouldBeHex() {
        assertEquals("0x" + Long.toHexString(ShardedId.fromId(255L).value()),
                ShardedId.fromId(255L).toString());
    }
    @Test
    void shardIdShouldEquals(){
        ShardId shardId = ShardId.of(12345);
        ShardedId a = ShardedId.of(shardId, 22222222);
        ShardedId b = ShardedId.of(shardId, 33333333);

        assertTrue(a.shardEquals(shardId));
        assertTrue(a.shardEquals(b));
    }

    @Test
    void shardIdShouldNotEquals(){
        ShardedId a = ShardedId.of(123, 22222222);
        ShardedId b = ShardedId.of(456, 33333333);

        assertFalse(a.shardEquals(ShardId.of(456)));
        assertFalse(a.shardEquals(b));
    }
}
