package com.ovelin.mall.id.generator.starter.domain.module;

import org.junit.jupiter.api.Test;

public class SegmentTest {

    @Test
    void testNext(){
        Segment segment = Segment.fromStartAndSize(1, 1000000);
        segment.next();

        for (int i = 0; i < 100; i++) {
            Thread t2 = Thread.ofVirtual()
                    .name("my-vt-", 0)
                    .start(() -> {
//                        System.out.println("② " + Thread.currentThread() + " next: " + segment.next());
                    });
            try {
                t2.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
