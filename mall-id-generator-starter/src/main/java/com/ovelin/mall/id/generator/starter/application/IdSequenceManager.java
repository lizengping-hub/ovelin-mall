package com.ovelin.mall.id.generator.starter.application;

public class IdSequenceManager {
    public long nextValue(String sequenceName) {

        return (long) (Math.random() * 1000000); // Example: return a random number as the next value
    }
}
