package com.ovelin.mall.auth.domain.model.identityIdentifier;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;
import com.ovelin.mall.auth.domain.model.valueobject.UserName;
import org.apache.catalina.User;

public class UserNameIdentifier extends IdentityIdentifier{
    private final UserName userName;
    private UserNameIdentifier(UserName userName){
        this.userName = userName;
    }
    @Override
    public IdentityType type() {
        return IdentityType.USERNAME;
    }

    @Override
    public String value() {
        return userName.value();
    }
    public static UserNameIdentifier from(UserName userName){
        return new UserNameIdentifier(userName);
    }
    public static UserNameIdentifier from(String value){
        return new UserNameIdentifier(UserName.from(value));
    }
    public static UserNameIdentifier reconstitute(String value){
        return new UserNameIdentifier(UserName.reconstitute(value));
    }
}
