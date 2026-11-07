package com.mall.mgr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.mgr.domain.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
