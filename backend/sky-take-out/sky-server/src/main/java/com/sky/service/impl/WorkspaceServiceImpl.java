package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台业务实现类。
 */
@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 根据时间段统计营业数据。
     *
     * @param begin 开始时间
     * @param end 结束时间
     * @return 营业数据
     */
    @Override
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);

        Integer totalOrderCount = orderMapper.countByMap(map);

        map.put("status", Orders.COMPLETED);
        Double turnover = orderMapper.sumByMap(map);
        turnover = turnover == null ? 0.0 : turnover;

        Integer validOrderCount = orderMapper.countByMap(map);
        Double unitPrice = 0.0;
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0 && validOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
            unitPrice = turnover / validOrderCount;
        }

        Integer newUsers = userMapper.countByMap(map);
        log.info("工作台营业数据统计完成：totalOrderCount={}, validOrderCount={}, turnover={}, newUsers={}",
                totalOrderCount, validOrderCount, turnover, newUsers);
        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }

    /**
     * 根据时间段统计每日营业数据列表。
     *
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 每日营业数据列表
     */
    @Override
    public List<BusinessDataVO> getBusinessDataList(LocalDateTime beginTime, LocalDateTime endTime) {
        List<BusinessDataVO> businessDataList = new ArrayList<>();
        LocalDate beginDate = beginTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();

        for (LocalDate currentDate = beginDate; !currentDate.isAfter(endDate); currentDate = currentDate.plusDays(1)) {
            LocalDateTime dayBegin = LocalDateTime.of(currentDate, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(currentDate, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", dayBegin);
            map.put("end", dayEnd);

            Integer totalOrderCount = orderMapper.countByMap(map);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            Integer validOrderCount = orderMapper.countByMap(map);
            Integer newUsers = userMapper.countByMap(map);

            double orderCompletionRate = totalOrderCount == null || totalOrderCount == 0
                    ? 0.0
                    : validOrderCount.doubleValue() / totalOrderCount;
            double unitPrice = validOrderCount == null || validOrderCount == 0
                    ? 0.0
                    : turnover / validOrderCount;

            businessDataList.add(BusinessDataVO.builder()
                    .dateStr(currentDate.toString())
                    .turnover(turnover)
                    .validOrderCount(validOrderCount)
                    .orderCompletionRate(orderCompletionRate)
                    .unitPrice(unitPrice)
                    .newUsers(newUsers)
                    .build());
        }

        return businessDataList;
    }

    /**
     * 查询今日订单概览。
     *
     * @return 订单概览
     */
    @Override
    public OrderOverViewVO getOrderOverView() {
        Map map = new HashMap();
        map.put("begin", LocalDateTime.now().with(LocalTime.MIN));

        map.put("status", Orders.TO_BE_CONFIRMED);
        Integer waitingOrders = orderMapper.countByMap(map);
        map.put("status", Orders.CONFIRMED);
        Integer deliveredOrders = orderMapper.countByMap(map);
        map.put("status", Orders.COMPLETED);
        Integer completedOrders = orderMapper.countByMap(map);
        map.put("status", Orders.CANCELLED);
        Integer cancelledOrders = orderMapper.countByMap(map);
        map.put("status", null);
        Integer allOrders = orderMapper.countByMap(map);

        log.info("工作台订单概览统计完成：waitingOrders={}, deliveredOrders={}, completedOrders={}, cancelledOrders={}, allOrders={}",
                waitingOrders, deliveredOrders, completedOrders, cancelledOrders, allOrders);
        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    /**
     * 查询菜品总览。
     *
     * @return 菜品概览
     */
    @Override
    public DishOverViewVO getDishOverView() {
        Map map = new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = dishMapper.countByMap(map);
        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = dishMapper.countByMap(map);
        log.info("工作台菜品总览统计完成：sold={}, discontinued={}", sold, discontinued);
        return DishOverViewVO.builder().sold(sold).discontinued(discontinued).build();
    }

    /**
     * 查询套餐总览。
     *
     * @return 套餐概览
     */
    @Override
    public SetmealOverViewVO getSetmealOverView() {
        Map map = new HashMap();
        map.put("status", StatusConstant.ENABLE);
        Integer sold = setmealMapper.countByMap(map);
        map.put("status", StatusConstant.DISABLE);
        Integer discontinued = setmealMapper.countByMap(map);
        log.info("工作台套餐总览统计完成：sold={}, discontinued={}", sold, discontinued);
        return SetmealOverViewVO.builder().sold(sold).discontinued(discontinued).build();
    }
}
