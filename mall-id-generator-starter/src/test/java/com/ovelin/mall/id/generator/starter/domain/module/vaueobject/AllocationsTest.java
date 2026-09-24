package com.ovelin.mall.id.generator.starter.domain.module.vaueobject;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.Allocation;
import com.ovelin.mall.id.generator.starter.domain.module.valueobject.Allocations;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 Allocations.iterator()。
 * 通过 Allocations.add(Allocation) 构造测试数据。
 */
class AllocationsTest {

    private static void addAll(Allocations target, List<Allocation> data) {
        for (Allocation a : data) {
            target.add(a);
        }
    }

    private static List<Long> drain(Iterator<Long> it) {
        List<Long> result = new ArrayList<>();
        while (it.hasNext()) {
            result.add(it.next());
        }
        return result;
    }

    @Test
    void emptyAllocations_hasNoElements() {
        Allocations allocations = new Allocations();

        Iterator<Long> it = allocations.iterator();

        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void singleAllocation_singleValue() {
        Allocations allocations = new Allocations();
        addAll(allocations, List.of(new Allocation(10L, 1)));

        List<Long> values = drain(allocations.iterator());

        assertEquals(List.of(10L), values);
    }

    @Test
    void singleAllocation_multipleValues() {
        Allocations allocations = new Allocations();
        addAll(allocations, List.of(new Allocation(5L, 4))); // 5,6,7,8

        List<Long> values = drain(allocations.iterator());

        assertEquals(List.of(5L, 6L, 7L, 8L), values);
    }

    @Test
    void multipleAllocations_areFlattenedInOrder() {
        Allocations allocations = new Allocations();
        addAll(allocations, List.of(
                new Allocation(1L, 3),   // 1,2,3
                new Allocation(100L, 2), // 100,101
                new Allocation(50L, 1)   // 50
        ));

        List<Long> values = drain(allocations.iterator());

        assertEquals(List.of(1L, 2L, 3L, 100L, 101L, 50L), values);
    }

    @Test
    void skipsEmptyAllocations_zeroCount() {
        Allocations allocations = new Allocations();
        addAll(allocations, List.of(
                new Allocation(1L, 0),   // 空区间,应被跳过
                new Allocation(7L, 2),   // 7,8
                new Allocation(20L, 0),  // 空区间,应被跳过
                new Allocation(9L, 1)    // 9
        ));

        List<Long> values = drain(allocations.iterator());

        assertEquals(List.of(7L, 8L, 9L), values);
    }

    @Test
    void allAllocationsEmpty_hasNoElements() {
        Allocations allocations = new Allocations();
        addAll(allocations, List.of(
                new Allocation(1L, 0),
                new Allocation(2L, 0)
        ));

        Iterator<Long> it = allocations.iterator();

        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void hasNext_isIdempotent_doesNotConsumeElements() {
        Allocations allocations = new Allocations();
        addAll(allocations, List.of(new Allocation(1L, 2))); // 1,2

        Iterator<Long> it = allocations.iterator();

        assertTrue(it.hasNext());
        assertTrue(it.hasNext()); // 重复调用不应改变状态
        assertEquals(1L, it.next());
        assertTrue(it.hasNext());
        assertEquals(2L, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void next_throwsAfterExhausted() {
        Allocations allocations = new Allocations();
        addAll(allocations, List.of(new Allocation(1L, 1))); // 1

        Iterator<Long> it = allocations.iterator();
        assertEquals(1L, it.next());

        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void multipleIterators_areIndependent() {
        Allocations allocations = new Allocations();
        addAll(allocations, List.of(new Allocation(1L, 3))); // 1,2,3

        Iterator<Long> it1 = allocations.iterator();
        Iterator<Long> it2 = allocations.iterator();

        assertEquals(1L, it1.next());
        assertEquals(2L, it1.next());

        // it2 应该独立地从头开始
        assertEquals(1L, it2.next());
    }
}
