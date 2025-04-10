package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     *
     * @param shoppingCartDTO
     */
    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

        // 设置用户id
        shoppingCart.setUserId(BaseContext.getCurrentId());
        // 判断是否已经存在该商品
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list != null && list.size() > 0) { // 已存在
            ShoppingCart shoppingCart1 = list.get(0);
            shoppingCart1.setNumber(shoppingCart1.getNumber() + 1);
            shoppingCartMapper.updateNumber(shoppingCart1);
        } else { // 不存在

            // 判断是菜品还是套餐
            List<Long> ids = new ArrayList<>();
            if (shoppingCart.getDishId() != null) {
                ids.add(shoppingCart.getDishId());
                List<Dish> dishList = dishMapper.getById(ids);
                shoppingCart.setName(dishList.get(0).getName());
                shoppingCart.setImage(dishList.get(0).getImage());
                shoppingCart.setAmount(dishList.get(0).getPrice());

            } else {
                ids.add(shoppingCart.getSetmealId());
                List<Setmeal> setmealList = setmealMapper.getById(ids);
                shoppingCart.setName(setmealList.get(0).getName());
                shoppingCart.setImage(setmealList.get(0).getImage());
                shoppingCart.setAmount(setmealList.get(0).getPrice());
            }

            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 查询购物车
     * @param
     * @return
     */
    @Override
    public List<ShoppingCart> list() {
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        return shoppingCartList;
    }

    /**
     * 清空购物车
     */
    @Override
    public void clean() {
        shoppingCartMapper.cleanByUserId(BaseContext.getCurrentId());
    }

    /**
     * 删除购物车中的某一项
     * @param shoppingCartDTO
     */
    @Override
    public void sub(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

        // 设置用户id
        shoppingCart.setUserId(BaseContext.getCurrentId());

        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        ShoppingCart shoppingCart1 = list.get(0);
        if (shoppingCart1.getNumber() > 1) { // 数量超过1则减1
            shoppingCart1.setNumber(shoppingCart1.getNumber() - 1);
            shoppingCartMapper.updateNumber(shoppingCart1);
        } else { // 数量为1则删除
            shoppingCartMapper.deleteById(shoppingCart1.getId());
        }
    }
}
