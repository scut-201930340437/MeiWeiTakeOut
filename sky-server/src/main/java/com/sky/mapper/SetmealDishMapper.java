package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品id查询关联的套餐
     * @param dishIds
     * @return
     */

    List<SetmealDish> getByDishId(List<Long> dishIds);

    /**
     * 根据菜品id查询套餐id
     * @param dishIds
     * @return
     */

    List<Long> getSetmealIdsByDishId(List<Long> dishIds);

    /**
     * 根据套餐id查询套餐菜品关系
     * @param id
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{id}")
    List<SetmealDish> getBySetmealId(Long id);

    List<SetmealDish> getBySetmealIds(List<Long> idsNeedDelete);

    /**
     * 批量插入套餐菜品关系数据
     * @param setmealDishList
     */
    void insertBatch(List<SetmealDish> setmealDishList);


    void deleteBySetmealIds(List<Long> ids);
}
