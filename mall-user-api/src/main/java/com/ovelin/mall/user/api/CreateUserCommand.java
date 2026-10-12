package com.ovelin.mall.user.api;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import lombok.Getter;

@Getter
public final class CreateUserCommand {
    private final ShardId shardId;
    private final String displayName;
    private final String registerSource;
    private final String avatarUrl;
    /*
        Gender 0: 未知; 1: 男; 2: 女
     */
    private final Integer gender;
    private final Integer birthYear;
    private final Integer birthMonth;
    private final Integer birthDay;

    private CreateUserCommand(Builder b){
        this.shardId = b.shardId;
        this.displayName = b.displayName;
        this.registerSource = b.registerSource;
        this.avatarUrl = b.avatarUrl;
        this.gender = b.gender;
        this.birthYear = b.birthYear;
        this.birthMonth = b.birthMonth;
        this.birthDay = b.birthDay;
    }
    public static Builder builder(String displayName, String registerSource){
        return new Builder(displayName, registerSource);
    }
    public static final class Builder{
        private ShardId shardId;
        private final String displayName;
        private final String registerSource;

        /*
         Gender 0: 未知; 1: 男; 2: 女
        */
        private String avatarUrl;
        private Integer gender;
        private Integer birthYear;
        private Integer birthMonth;
        private Integer birthDay;
        public Builder(String displayName, String registerSource){
            this.displayName = displayName;
            this.registerSource = registerSource;
        }
        public Builder shardId(ShardId shardId){
            this.shardId = shardId;
            return this;
        }
        public Builder avatarUrl(String avatarUrl){
            this.avatarUrl = avatarUrl;
            return this;
        }
        public Builder gender(int gender){
            this.gender = gender;
            return this;
        }
        public Builder birthYear(int birthYear){
            this.birthYear = birthYear;
            return this;
        }
        public Builder birthDay(int birthMonth, int birthDay){
            this.birthMonth = birthMonth;
            this.birthDay = birthDay;
            return this;
        }
        public CreateUserCommand build(){
            return new CreateUserCommand(this);
        }
    }
}
