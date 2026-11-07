package com.mall.mgr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.mgr.domain.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}