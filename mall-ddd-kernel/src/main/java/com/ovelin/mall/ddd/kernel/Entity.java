package com.ovelin.mall.ddd.kernel;

public interface Entity<T extends Identifier> {
    T getId();
}
