package com.ovelin.mall.user.domain;

import com.ovelin.mall.id.generator.api.BusinessType;
import com.ovelin.mall.id.generator.api.IdGenerationCommand;
import com.ovelin.mall.user.domain.model.valueobject.UserBusinessType;

public class UserIdGenerationCommand {
    public static IdGenerationCommand.Builder builder(){
        return IdGenerationCommand.builder(UserBusinessType.USER_BUSINESS_TYPE);
    }
}
