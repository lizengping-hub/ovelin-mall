package com.ovelin.mall.sharding.starter.po;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<OrderPO> {
    // 空的也行，selectById/insert/updateById/selectList(wrapper)... 全部现成
}
