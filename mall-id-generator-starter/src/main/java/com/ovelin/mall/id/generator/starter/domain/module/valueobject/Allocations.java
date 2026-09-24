package com.ovelin.mall.id.generator.starter.domain.module.valueobject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class Allocations {
    private final List<Allocation> allocations;

    public Allocations() {
        this.allocations = new ArrayList<>();
    }
    public void add(Allocation allocation){
        this.allocations.add(allocation);
    }
    public List<Allocation> getAllocations() {
        return allocations;
    }
    public int idCount() {
        return allocations.stream().mapToInt(Allocation::count).sum();
    }

    public long[] toArray() {
        long[] result = new long[idCount()];
        int index = 0;
        for (Allocation allocation : allocations) {
            for (int i = 0; i < allocation.count(); i++) {
                result[index++] = allocation.start() + i;
            }
        }
        return result;
    }
    public List<Long> toList() {
        List<Long> result = new ArrayList<>(idCount());
        for (Allocation allocation : allocations) {
            for (int i = 0; i < allocation.count(); i++) {
                result.add(allocation.start() + i);
            }
        }
        return result;
    }
    public Iterator<Long> iterator() {
        return new Iterator<>() {

            private int allocationIndex = 0;
            private int offset = 0;

            @Override
            public boolean hasNext() {
                while (allocationIndex < allocations.size()) {
                    Allocation allocation = allocations.get(allocationIndex);
                    if (offset < allocation.count()) {
                        return true;
                    }
                    allocationIndex++;
                    offset = 0;
                }
                return false;
            }

            @Override
            public Long next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Allocation allocation = allocations.get(allocationIndex);
                long id = allocation.start() + offset;
                offset++;
                return id;
            }
        };
    }
}
