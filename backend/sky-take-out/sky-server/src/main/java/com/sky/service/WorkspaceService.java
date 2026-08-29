package com.sky.service;

import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作台业务层。
 */
public interface WorkspaceService {

    /**
     * 根据时间段统计营业数据。
     *
     * @param begin 开始时间
     * @param end 结束时间
     * @return 营业数据
     */
    BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end);

    /**
     * 查询订单管理数据。
     *
     * @return 订单概览
     */
    OrderOverViewVO getOrderOverView();

    /**
     * 查询菜品总览。
     *
     * @return 菜品概览
     */
    DishOverViewVO getDishOverView();

    /**
     * 查询套餐总览。
     *
     * @return 套餐概览
     */
    SetmealOverViewVO getSetmealOverView();

    /**
     * 根据时间段统计每日营业数据列表。
     *
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 每日营业数据列表
     */
    List<BusinessDataVO> getBusinessDataList(LocalDateTime beginTime, LocalDateTime endTime);
}
