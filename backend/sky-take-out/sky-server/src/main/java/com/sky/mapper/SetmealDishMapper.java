package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 套餐与菜品的映射关系Mapper
 */
@Mapper
public interface SetmealDishMapper {


    List<Long> getSetmealIdsByDishId(@Param("dishIds") List<Long> dishIds);

}
