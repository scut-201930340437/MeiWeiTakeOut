package com.sky.controller.admin;

import com.sky.constant.ShopStatusConstant;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Slf4j
@Api(tags = "店铺相关接口")
public class ShopController {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 修改店铺状态
     *
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @ApiOperation("修改店铺状态")
    public Result setStatus(@PathVariable Integer status) {
        log.info("设置店铺状态 {}", status == ShopStatusConstant.RUNNING ? ShopStatusConstant.RUNNING_STR : ShopStatusConstant.CLOSING_STR);

        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set(ShopStatusConstant.STATUS_STR, status);

        return Result.success();
    }

    /**
     * 获取店铺状态
     *
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("获取店铺状态")
    public Result getStatus() {
        log.info("获取店铺状态");

        ValueOperations valueOperations = redisTemplate.opsForValue();
        Integer status = (Integer) valueOperations.get(ShopStatusConstant.STATUS_STR);

        return Result.success(status == ShopStatusConstant.RUNNING ? ShopStatusConstant.RUNNING_STR : ShopStatusConstant.CLOSING_STR);
    }
}
