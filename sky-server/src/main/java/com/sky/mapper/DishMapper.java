package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DishMapper {

    /**
     * 新增菜品
     *
     * @param dish
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Dish dish);

    /**
     * 根据分类id查询菜品数量
     *
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 分页查询菜品数据
     *
     * @param dishPageQueryDTO
     * @return
     */
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根据id查询菜品数据
     *
     * @param ids
     * @return
     */
    List<Dish> getById(List<Long> ids);

    /**
     * 根据条件查询菜品数据
     * @param dish
     * @return
     */
    List<Dish> getByConditions(Dish dish);

    /**
     * 批量删除菜品
     *
     * @param ids
     */

    void deleteByIds(List<Long> ids);

    /**
     * 根据id修改菜品
     *
     * @param dish
     */
    @AutoFill(value = OperationType.UPDATE)
    void updateWithFlavor(Dish dish);

    /**
     * 根据条件统计菜品数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
