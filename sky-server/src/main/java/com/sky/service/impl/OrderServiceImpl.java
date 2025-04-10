package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 提交订单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        Long userId = BaseContext.getCurrentId();

        // 处理异常，无地址 购物车无商品
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 查购物车
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID); // 设置支付状态为未支付
        orders.setStatus(Orders.PENDING_PAYMENT); // 设置订单状态为待支付
        orders.setNumber(String.valueOf(System.currentTimeMillis()));  // 订单号
        orders.setPhone(addressBook.getPhone());
        orders.setAddress(addressBook.getDetail());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        orderMapper.insert(orders);


        // 订单明细表插入多条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        shoppingCartList.forEach(item -> {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(item, orderDetail);
            orderDetail.setOrderId(orders.getId());

            orderDetailList.add(orderDetail);
        });
        orderDetailMapper.insertBatch(orderDetailList);

        // 清空购物车
        shoppingCartMapper.cleanByUserId(userId);

        // 封装VO对象并返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {
        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);


        //提醒商家有订单
        Map map = new HashMap<>();
        map.put("type", 1);
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号"+outTradeNo);
        webSocketServer.sendToAllClient(JSON.toJSONString(map));

    }

    /**
     * 分页查询订单
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuerty(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        if (page != null && page.getTotal() > 0) {
            List<Orders> records = page.getResult();
            List<OrderVO> orderVOList = new ArrayList<>();
            for (Orders order : records) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);

                // 查询订单明细
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(order.getId());

                orderVO.setOrderDetailList(orderDetailList);

                orderVOList.add(orderVO);
            }
            return new PageResult(page.getTotal(), orderVOList);
        }

        return null;
    }

    /**
     * 根据id查询订单
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO getById(Long id) {
        Orders order = orderMapper.getById(id);

        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 催单
     * @param id
     */
    @Override
    public void reminder(Long id) {
        Orders orderExist = orderMapper.getById(id);

        // 判断订单是否存在
        if (orderExist == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 判断订单是否可催单，只有待处理的订单可以催单
        if (orderExist.getStatus() != Orders.TO_BE_CONFIRMED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 构造消息，提醒商家
        Map map = new HashMap<>();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("content", "订单号" + orderExist.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 用户取消订单
     *
     * @param id
     * @throws Exception
     */
    @Override
    public void userCancel(Long id) throws Exception {
        Orders orderExist = orderMapper.getById(id);

        // 判断订单是否存在
        if (orderExist == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 判断订单是否可取消，只有待付款、待接单的订单可以直接取消
        if (orderExist.getStatus() > Orders.TO_BE_CONFIRMED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders order = new Orders();
        order.setId(id);

        // 如果是待处理，则需要退款
        if (orderExist.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //调用微信支付退款接口
            weChatPayUtil.refund(
                    orderExist.getNumber(), //商户订单号
                    orderExist.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01));//原订单金额

            // 修改支付状态为退款
            order.setPayStatus(Orders.REFUND);
        }

        order.setStatus(Orders.CANCELLED);
        order.setCancelReason("用户取消");
        order.setCancelTime(LocalDateTime.now());
        orderMapper.update(order);
    }

    /**
     * 再来一单
     *
     * @param id
     */
    @Override
    public void repetition(Long id) {
        Orders order = orderMapper.getById(id);

        if (order != null) {
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
            if (orderDetailList != null && orderDetailList.size() > 0) {
                // 订单明细表插入多条数据
                List<ShoppingCart> shoppingCartList = new ArrayList<>();
                orderDetailList.forEach(item -> {
                    ShoppingCart shoppingCart = new ShoppingCart();
                    BeanUtils.copyProperties(item, shoppingCart);
                    shoppingCart.setUserId(BaseContext.getCurrentId());
                    shoppingCart.setCreateTime(LocalDateTime.now());

                    shoppingCartList.add(shoppingCart);
                });
                shoppingCartMapper.insertBatch(orderDetailList); // 批量插入购物车
            }
        }
        return;
    }

    /**
     * 条件查询订单
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuertyOnCondition(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        if (page != null && page.getTotal() > 0) {
            List<Orders> records = page.getResult();
            List<OrderVO> orderVOList = new ArrayList<>();
            for (Orders order : records) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);

                // 查询订单明细
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(order.getId());

                if (orderDetailList != null && orderDetailList.size() > 0) {
                    List<String> orderDishList = new ArrayList<>();
                    orderDetailList.forEach(item -> {
                        String orderDish = item.getName() + "*" + item.getNumber();
                        orderDishList.add(orderDish);
                    });
                    orderVO.setOrderDishes(String.join("", orderDishList));
                }

                orderVOList.add(orderVO);
            }
            return new PageResult(page.getTotal(), orderVOList);
        }

        return null;
    }

    /**
     * 统计订单数量
     *
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders order = new Orders();
        order.setId(ordersConfirmDTO.getId());
        order.setStatus(Orders.CONFIRMED);

        orderMapper.update(order);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        Orders order = orderMapper.getById(ordersRejectionDTO.getId());

        // 判断订单状态，待处理的订单才能拒绝
        if (order == null && !order.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 查看订单支付状态，如果已经支付，则退款
        if (order.getPayStatus().equals(Orders.PAID)) {
            weChatPayUtil.refund(
                    order.getNumber(),
                    order.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
        }

        //
        Orders orderUpdate = new Orders();
        orderUpdate.setId(ordersRejectionDTO.getId());
        orderUpdate.setStatus(Orders.CANCELLED);
        orderUpdate.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orderUpdate.setCancelTime(LocalDateTime.now());

        orderMapper.update(orderUpdate);
    }

    @Override
    public void adminCancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        Orders order = orderMapper.getById(ordersCancelDTO.getId());

        // 判断订单状态，待处理的订单才能拒绝
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 查看订单支付状态，如果已经支付，则退款
        if (order.getPayStatus().equals(Orders.PAID)) {
            weChatPayUtil.refund(
                    order.getNumber(),
                    order.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
        }

        //
        Orders orderUpdate = new Orders();
        orderUpdate.setId(ordersCancelDTO.getId());
        orderUpdate.setStatus(Orders.CANCELLED);
        orderUpdate.setCancelReason(ordersCancelDTO.getCancelReason());
        orderUpdate.setCancelTime(LocalDateTime.now());

        orderMapper.update(orderUpdate);
    }

    /**
     * 派送订单
     *
     * @param id
     */
    @Override
    public void delivery(Long id) {
        Orders order = orderMapper.getById(id);

        // 判断订单状态，已结单的订单才能派送中
        if (order == null && !order.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }


        Orders orderUpdate = new Orders();
        orderUpdate.setId(order.getId());
        orderUpdate.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orderUpdate);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    @Override
    public void complete(Long id) {
        Orders order = orderMapper.getById(id);

        // 判断订单状态，派送中的订单才能完成
        if (order == null && !order.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orderUpdate = new Orders();
        orderUpdate.setId(order.getId());
        orderUpdate.setStatus(Orders.COMPLETED);
        orderUpdate.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orderUpdate);
    }
}
