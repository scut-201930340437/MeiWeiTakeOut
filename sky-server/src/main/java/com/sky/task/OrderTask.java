package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单定时任务
 */
@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    /**
     * 处理超时未支付订单
     */
    @Scheduled(cron = "0 * * * * ?") // 每分钟触发一次
    public void orderTimeOutCancelTask() {
        log.info("处理超时订单，当前时间：{}", System.currentTimeMillis());

        Integer status = Orders.PENDING_PAYMENT;
        LocalDateTime orderTime = LocalDateTime.now().plusMinutes(-15);

        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeOut(status, orderTime);

        if (ordersList != null && ordersList.size() > 0){
            ordersList.forEach(orders -> {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
            });

            orderMapper.updateBatch(ordersList);
        }
    }

    /**
     * 处理处于派送中状态的订单
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每分钟触发一次
    public void processDeliveryOrderTask() {
        log.info("处理处于派送中状态的订单");

        Integer status = Orders.DELIVERY_IN_PROGRESS;
        LocalDateTime orderTime = LocalDateTime.now().plusHours(-1);

        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeOut(status, orderTime);

        if (ordersList != null && ordersList.size() > 0){
            ordersList.forEach(orders -> {
                orders.setStatus(Orders.COMPLETED);
            });

            orderMapper.updateBatch(ordersList);
        }

    }
}
