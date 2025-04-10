package com.sky.controller.user;


import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端菜品接口")
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        log.info("查询菜品");

        // 使用redis缓存菜品数据
        String key = "dish_" + categoryId;
        // 先尝试获取缓存数据
        List<DishVO> dishVOList = (List<DishVO>) redisTemplate.opsForValue().get(key);

        if (dishVOList != null && dishVOList.size() > 0){
            return Result.success(dishVOList);
        }

        // 缓存中没有数据
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);
        dishVOList = dishService.getByConditionsWithFlavor(dish);

        // 放入缓存
        redisTemplate.opsForValue().set(key, dishVOList);

        return Result.success(dishVOList);
    }

}
