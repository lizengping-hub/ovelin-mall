package com.ovelin.mall.auth.application.port;

import com.ovelin.mall.auth.domain.model.valueobject.EncodedPassword;
import com.ovelin.mall.auth.domain.model.valueobject.Password;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface OvelinPasswordEncoder {

    @NonNull EncodedPassword encode(@NonNull Password password);


    boolean matches(@NonNull Password password, @NonNull EncodedPassword encodedPassword);

    boolean upgradeEncoding(@NonNull EncodedPassword encodedPassword);
}
