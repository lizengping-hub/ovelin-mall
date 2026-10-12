package com.ovelin.mall.common.mybatis.plus.kernal.ovelinMybatisPlusAutoConfiguration;

import com.ovelin.mall.common.mybatis.plus.kernal.SqlCollectingInterceptor;
import com.ovelin.mall.common.mybatis.plus.kernal.UserPO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class OvelinMybatisPlusAutoConfigurationIT {
    @Autowired
    SqlCollectingInterceptor sqlCollectingInterceptor;
    @Autowired
    UserUpdateMapper userUpdateMapper;

    @Test
    void insertQueryAndUpdate(){
        UserPO u = new UserPO();
        u.setUsername("dave");
        u.setAge(18);
        assertThat(u.getVersion()).isEqualTo(0);
        userUpdateMapper.insert(u);

        sqlCollectingInterceptor.clear();
        userUpdateMapper.updateById(u);
        assertThat(sqlCollectingInterceptor.getSqls().size()).isEqualTo(1);
        String sql = sqlCollectingInterceptor.getSqls().getFirst();
        assertThat(sql).contains("AND version=?");

        UserPO selected = userUpdateMapper.selectById(u.getId());
        assertThat(selected).isNotNull();
        assertThat(selected.getVersion()).isEqualTo(1);

        selected.setUsername("andy");
        userUpdateMapper.updateById(selected);
        assertThat(selected.getVersion()).isEqualTo(2);
        assertThat(selected.getUsername()).isEqualTo("andy");

        UserPO selectedAfterUpdate = userUpdateMapper.selectById(u.getId());
        assertThat(selectedAfterUpdate).isNotNull();
        assertThat(selectedAfterUpdate.getVersion()).isEqualTo(2);
    }
}
