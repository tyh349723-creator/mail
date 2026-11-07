package com.mall.mgr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.mgr.domain.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
