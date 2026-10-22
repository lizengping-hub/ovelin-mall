package com.ovelin.mall.auth.infrastructure.persistence.mapper;

import com.ovelin.mall.auth.infrastructure.persistence.AuthIntegrationTestSupport;
import com.ovelin.mall.auth.infrastructure.persistence.po.LoginAccountPO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LoginAccountMapperIT extends AuthIntegrationTestSupport {
    @Autowired
    LoginAccountMapper mapper;
    @Test
    void insertAndSelect(){
        LoginAccountPO po = new LoginAccountPO();
        po.setUserId(newId("user").value());
        mapper.insert(po);

        LoginAccountPO selected = mapper.selectByUserId(po.getUserId());

        assertThat(selected.getUserId()).isEqualTo(po.getUserId());
        assertThat(selected.getUpdatedAt()).isNotNull();
        assertThat(selected.getCreatedAt()).isNotNull();
        assertThat(selected.getVersion()).isZero();
    }
    @Test
    void insertUserIdIsNull(){
        LoginAccountPO po = new LoginAccountPO();
        assertThatThrownBy(()->mapper.insert(po));

    }
    @Test
    void update(){
        LoginAccountPO po = new LoginAccountPO();
        po.setUserId(newId("user").value());
        mapper.insert(po);

        LoginAccountPO selected = mapper.selectByUserId(po.getUserId());

        mapper.updateByUserId(selected);

        assertThat(selected.getVersion()).isEqualTo(1);
    }
    @Test
    void delete(){
        LoginAccountPO po = new LoginAccountPO();
        po.setUserId(newId("user").value());
        mapper.insert(po);

        mapper.deleteByUserId(po.getUserId());

        assertThat(mapper.selectByUserId(po.getUserId())).isNull();
    }
}
