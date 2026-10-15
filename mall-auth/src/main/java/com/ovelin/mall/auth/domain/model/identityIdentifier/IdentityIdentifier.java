package com.ovelin.mall.auth.domain.model.identityIdentifier;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;

public abstract class IdentityIdentifier {
    private Integer hashCode;

    public abstract IdentityType type();

    public abstract String value();

    @Override
    public int hashCode(){
        if (hashCode == null){
            hashCode = (type().getCode()+value()).hashCode();
        }
        return hashCode;
    }
    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof IdentityIdentifier other)) return false;
        return this.type() == other.type() && this.value().equals(other.value());
    }
    @Override
    public String toString(){
        return "IdentityIdentifier[type = %s, value = %s]".formatted(type().getCode(), value());
    }
}