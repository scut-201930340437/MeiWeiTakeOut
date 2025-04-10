package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品和对应的口味
     *
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 插入菜品数据
        dishMapper.insert(dish);
        Long dishId = dish.getId();

        // 插入口味数据
        List<DishFlavor> flavorList = dishDTO.getFlavors();
        if (flavorList != null && flavorList.size() > 0) {
            flavorList.forEach(item -> {
                item.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavorList);
        }
    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);

        // 获取每个菜品的口味
        for (DishVO dishVO : page.getResult()) {
            List<DishFlavor> dishFlavors = dishFlavorMapper.queryByDishId(dishVO.getId());
            dishVO.setFlavors(dishFlavors);
        }
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 根据id查询菜品
     *
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        List<Long> ids = new ArrayList<>();
        ids.add(id);
        List<Dish> resDishes = dishMapper.getById(ids);
        if (resDishes.isEmpty()) {
            return null;
        } else {
            Dish dish = resDishes.get(0);
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);
            // 查询口味
            List<DishFlavor> flavors = dishFlavorMapper.queryByDishId(id);
            dishVO.setFlavors(flavors);

            return dishVO;
        }
    }


    /**
     * 根据条件查询菜品
     * @param dish
     * @return
     */
    @Override
    public List<DishVO> getByConditionsWithFlavor(Dish dish) {
        List<Dish> resDishList = dishMapper.getByConditions(dish);
        List<DishVO> resDishVOList = new ArrayList<>();
        if (resDishList.isEmpty()) {
            return null;
        } else {
            resDishList.forEach(item -> {
                Long id = item.getId();

                DishVO dishVO = new DishVO();
                BeanUtils.copyProperties(item, dishVO);
                // 查询口味
                List<DishFlavor> flavors = dishFlavorMapper.queryByDishId(id);
                dishVO.setFlavors(flavors);

                resDishVOList.add(dishVO);
            });


            return resDishVOList;
        }
    }


    /**
     * 批量删除菜品
     *
     * @param ids
     */
    @Transactional
    @Override
    public List<Long> delete(List<Long> ids) {
        // 判断菜品是否启售
        List<Long> idsNeedDelete = new ArrayList<>();
        List<Dish> dishList = dishMapper.getById(ids);
        dishList.forEach(item -> {
            if (item.getStatus() == StatusConstant.DISABLE) {
                idsNeedDelete.add(item.getId());
            }
        });

        // 判断菜品是否被套餐关联
        List<SetmealDish> setmealDishList = setmealDishMapper.getByDishId(idsNeedDelete);
        List<Long> idsWithSetmeal = new ArrayList<>();
        setmealDishList.forEach(item -> {
            idsWithSetmeal.add(item.getDishId());
        });

        // 从idsNoSale中移除已关联套餐的菜品id
        idsNeedDelete.removeAll(idsWithSetmeal);

        // 没有被删除的菜品id
        ids.removeAll(idsNeedDelete);

        // 删除
        dishMapper.deleteByIds(idsNeedDelete);
        dishFlavorMapper.deleteByDishIds(idsNeedDelete);

        return ids;
    }

    /**
     * 修改菜品
     *
     * @param dishDTO
     */
    @Transactional
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 先更新菜品属性
        dishMapper.updateWithFlavor(dish);

        // 对于口味，先删除到导入
        List<Long> ids = new ArrayList<>();
        ids.add(dishDTO.getId());

        // 先删除口味
        dishFlavorMapper.deleteByDishIds(ids);
        // 重新插入
        List<DishFlavor> flavors = dishDTO.getFlavors();

        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(item -> {
                item.setDishId(dishDTO.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }

    }

    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.updateWithFlavor(dish);
    }
}
