package com.ovelin.mall.auth.infrastructure.persistence.convertor;

import com.ovelin.mall.auth.domain.model.aggregate.LoginAccount;
import com.ovelin.mall.auth.infrastructure.persistence.po.LoginAccountPO;

public final class LoginAccountPOConverter {
    public static LoginAccountPO toPO(LoginAccount account){
        LoginAccountPO po = new LoginAccountPO();
        po.setUserId(account.getUserId().value());
        po.setVersion(account.getVersion());
        return po;
    }
}
