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
 * 定时任务类，处理订单超时未支付的订单等等
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理订单超时未支付的订单
     */
    @Scheduled(cron = "0 0/1 * * * ?") // 每分钟执行一次
    public void processTimeoutOrders() {
        log.info("处理订单超时未支付的订单: {}", LocalDateTime.now());
        // 获取当前时间的前15分钟
        LocalDateTime now = LocalDateTime.now().plusMinutes(-15);
        List<Orders> ordersList = (List<Orders>) orderMapper.getOrdersByStatusAndCreateTime(Orders.PENDING_PAYMENT, now);

        for (Orders orders : ordersList) {
            // 处理订单超时未支付的逻辑
            orders.setStatus(Orders.CANCELLED);
            orders.setCancelTime(LocalDateTime.now());
            orders.setCancelReason("订单超时未支付，系统自动取消");
            orderMapper.update(orders);
        }
    }

    /**
     * 处理一直处于派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行一次
    public void processUnDeliveredOrders() {
        log.info("处理一直处于派送中的订单: {}", LocalDateTime.now());
        // 获取当前时间的前15分钟
        LocalDateTime now = LocalDateTime.now().plusHours(-1);
        List<Orders> ordersList = (List<Orders>) orderMapper.getOrdersByStatusAndCreateTime(Orders.DELIVERY_IN_PROGRESS, now);

        for (Orders orders : ordersList) {
            // 处理一直处于派送中的订单逻辑
            orders.setStatus(Orders.COMPLETED);
            orders.setDeliveryTime(LocalDateTime.now());
            orderMapper.update(orders);
        }
   }

}
