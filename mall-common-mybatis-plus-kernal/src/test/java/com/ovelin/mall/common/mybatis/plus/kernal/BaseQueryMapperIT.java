package com.ovelin.mall.common.mybatis.plus.kernal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class BaseQueryMapperIT {
    @Autowired
    UserQueryMapper userQueryMapper;

    @Test
    void insertThrowException(){
        UserPO u = new UserPO();
        u.setUsername("dave");
        u.setAge(18);
        assertThrows(UnsupportedOperationException.class, ()-> userQueryMapper.insert(u));

    }
    @Test
    void insertAndQuery() {

        assertThat(userQueryMapper.selectById(1).getUsername()).isEqualTo("alice");
    }

    @Test
    void wrapperQuery() {
        List<UserPO> list = userQueryMapper.selectList(
                new LambdaQueryWrapper<UserPO>().ge(UserPO::getAge, 25));
        assertThat(list);
    }
}
