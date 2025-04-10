package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /**
     * 新增菜品的口味数据
     *
     * @param dishFlavor
     */
    @Insert("insert into dish_flavor (dish_id, name, value)" +
            "values " +
            "(#{dishId}, #{name}, #{value})")
    void insert(DishFlavor dishFlavor);

    /**
     * 批量插入口味
     * @param flavorList
     */
    void insertBatch(List<DishFlavor> flavorList);

    /**
     * 根据菜品id查询口味
     * @param dishId
     */
    @Select("select * from dish_flavor where dish_id = #{dishId}")
    List<DishFlavor> queryByDishId(Long dishId);

    /**
     * 批量删除口味
     * @param dishIds
     */
    void deleteByDishIds(List<Long> dishIds);
}
