package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
@Api(tags = "套餐接口")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    /**
     * 新增套餐和对应的菜品
     *
     * @param setmealDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增套餐")
    @CacheEvict(cacheNames = "setmealCache", key = "#setmealDIO.categoryId")
    public Result save(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐:{}", setmealDTO);

        setmealService.saveWithDish(setmealDTO);
        return Result.success();
    }

    /**
     * 套餐分页查询
     *
     * @param setmealPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("套餐分页查询:{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.page(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据id查询菜品
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")
    public Result<SetmealVO> getByIdWithFlavor(@PathVariable Long id) {
        log.info("根据id查询套餐:{}", id);

        SetmealVO setmealVO = setmealService.getByIdWithDish(id);
        if (setmealVO == null) {
            return Result.error("套餐不存在");
        } else {
            return Result.success(setmealVO);
        }
    }

    /**
     * 批量删除菜品
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("批量删除套餐")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result<List<Long>> delete(@RequestParam List<Long> ids) {
        log.info("批量删除套餐:{}", ids);

        List<Long> idsNoDelete = setmealService.delete(ids);
        return Result.success(idsNoDelete);
    }

    @PostMapping("/status/{status}")
    @ApiOperation("启用or禁用套餐")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result startOrstop(@PathVariable Integer status, Long id) {
        log.info("启用或禁用套餐 {} {}", status, id);
        setmealService.startOrstop(status, id);
        return Result.success();
    }

    /**
     * 修改菜品
     * @param setmealDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result update(@RequestBody SetmealDTO setmealDTO){
        log.info("修改菜品:{}", setmealDTO);

        setmealService.updateWithDish(setmealDTO);
        return Result.success();
    }
}
