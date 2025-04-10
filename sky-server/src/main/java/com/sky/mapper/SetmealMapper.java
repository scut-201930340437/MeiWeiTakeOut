package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SetmealMapper {

    /**
     * 新增菜品
     *
     * @param setmeal
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Setmeal setmeal);

    /**
     * 根据分类id查询菜品数量
     *
     * @param categoryId
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 分页查询菜品数据
     *
     * @param setmealPageQueryDTO
     * @return
     */
    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 根据id查询菜品数据
     *
     * @param ids
     * @return
     */
    List<Setmeal> getById(List<Long> ids);

    /**
     * 根据条件查询菜品数据
     *
     * @param setmeal
     * @return
     */
    List<Setmeal> getByConditions(Setmeal setmeal);

    /**
     * 批量删除菜品
     *
     * @param ids
     */

    void deleteByIds(List<Long> ids);

    /**
     * 根据id修改菜品
     *
     * @param setmeal
     */
    @AutoFill(value = OperationType.UPDATE)
    void updateWithDish(Setmeal setmeal);

    /**
     * 根据条件统计菜品数量
     *
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
