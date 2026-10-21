package com.ovelin.mall.auth.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** 联合主键 (identity_type, identifier)，MyBatis-Plus 不支持联合主键，所以不加 @TableId，由 Mapper 手写 SQL。 */
@Getter
@Setter
@TableName("identity_lookup")
public class IdentityLookupPO {

    private String identityType;

    private String identifier;

    private Long userId;

    private int status;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Instant createdAt;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Instant updatedAt;

    @Override
    public int hashCode(){
        return toString().hashCode();
    }
    @Override
    public boolean equals(Object o){
        if (o instanceof IdentityLookupPO other){
            return this.toString().equals(o.toString());
        }
        return false;
    }
    @Override
    public String toString(){
        return identityType+":"+identifier;
    }
}