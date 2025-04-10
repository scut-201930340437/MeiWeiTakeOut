package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private DishMapper dishMapper;

    /**
     * 新增套餐和对应的菜品
     *
     * @param setmealDTO
     */
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        // 插入套餐数据
        setmealMapper.insert(setmeal);

        // 插入菜品数据
        List<SetmealDish> setmealDishList = setmealDTO.getSetmealDishes();
        // 设置套餐id
        if (setmealDishList != null && setmealDishList.size() > 0) {
            setmealDishList.forEach(item -> {
                item.setSetmealId(setmeal.getId());
            });
        }
        setmealDishMapper.insertBatch(setmealDishList);

    }

    /**
     * 套餐分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);

        // 获取每个套餐下的菜品
        for (SetmealVO setmealVO : page.getResult()) {
            List<SetmealDish> setmealDishList = setmealDishMapper.getBySetmealId(setmealVO.getId());
            setmealVO.setSetmealDishes(setmealDishList);
        }
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 根据id查询套餐
     *
     * @param id
     * @return SetmealVO
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        List<Long> ids = new ArrayList<>();
        ids.add(id);
        List<Setmeal> resSetmeals = setmealMapper.getById(ids);
        if (resSetmeals.isEmpty()) {
            return null;
        } else {
            Setmeal setmeal = resSetmeals.get(0);
            SetmealVO setmealVO = new SetmealVO();
            BeanUtils.copyProperties(setmeal, setmealVO);

            // 查询套餐下的菜品
            List<SetmealDish> setmealDishList = setmealDishMapper.getBySetmealId(id);
            setmealVO.setSetmealDishes(setmealDishList);

            return setmealVO;
        }
    }

    /**
     * 根据条件查询套餐
     *
     * @param setmeal
     * @return
     */
    @Override
    public List<Setmeal> getByCategoryId(Setmeal setmeal) {
        return setmealMapper.getByConditions(setmeal);
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @Override
    public List<DishItemVO> getDishItemById(Long id) {
        List<DishItemVO> dishItemVOList = new ArrayList<>();
        // 先查setmeal下的setmeal_dish
        List<SetmealDish> setmealDishList = setmealDishMapper.getBySetmealId(id);
        // 根据每个setmeal_dish 查dish，获取description和image
        setmealDishList.forEach(item -> {
            DishItemVO dishItemVO = new DishItemVO();
            dishItemVO.setCopies(item.getCopies());
            dishItemVO.setName(item.getName());

            List<Long> ids = new ArrayList<>();
            ids.add(item.getDishId());
            Dish dish = dishMapper.getById(ids).get(0);

            dishItemVO.setDescription(dish.getDescription());
            dishItemVO.setImage(dish.getImage());
        });
        return dishItemVOList;
    }


    /**
     * 批量删除套餐
     *
     * @param ids
     */
    @Transactional
    @Override
    public List<Long> delete(List<Long> ids) {
        // 判断套餐是否起售
        List<Long> idsNeedDelete = new ArrayList<>();
        List<Setmeal> setmealList = setmealMapper.getById(ids);
        setmealList.forEach(item -> {
            if (item.getStatus() == StatusConstant.DISABLE) {
                idsNeedDelete.add(item.getId());
            }
        });

        // 删除套餐
        setmealMapper.deleteByIds(idsNeedDelete);

        // 删除套餐-菜品的关联关系，不删除菜品
        setmealDishMapper.deleteBySetmealIds(idsNeedDelete);

        // 没有被删除的菜品id
        ids.removeAll(idsNeedDelete);

        return ids;
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO
     */
    @Transactional
    @Override
    public void updateWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        // 先更新菜品属性
        setmealMapper.updateWithDish(setmeal);

        // 对于关联的菜品，先删除到导入
        // 先删除菜品套餐关联关系
        List<Long> ids = new ArrayList<>();
        ids.add(setmealDTO.getId());
        setmealDishMapper.deleteBySetmealIds(ids);
        // 重新插入
        List<SetmealDish> setmealDishList = setmealDTO.getSetmealDishes();

        if (setmealDishList != null && !setmealDishList.isEmpty()) {
            setmealDishList.forEach(item -> {
                item.setSetmealId(setmealDTO.getId());
            });
            setmealDishMapper.insertBatch(setmealDishList);
        }
    }

    /**
     * 启用或禁用套餐
     *
     * @param status
     * @param id
     */
    @Override
    public void startOrstop(Integer status, Long id) {
        Setmeal setmeal = Setmeal.builder()
                .status(status)
                .id(id)
                .build();
        setmealMapper.updateWithDish(setmeal);
    }
}
