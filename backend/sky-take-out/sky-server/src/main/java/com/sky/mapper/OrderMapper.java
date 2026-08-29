package com.sky.mapper;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 订单数据处理层
 */
@Mapper
public interface OrderMapper {

    /**
     * 根据动态条件统计订单数量，供工作台使用。
     *
     * @param map 查询条件
     * @return 订单数量
     */
    Integer countByMap(Map map);

    /**
     * 根据动态条件统计营业额，供工作台使用。
     *
     * @param map 查询条件
     * @return 营业额
     */
    Double sumByMap(Map map);

    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     * @return
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 分页条件查询订单
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 根据订单id和用户id查询订单
     * @param id
     * @param userId
     * @return
     */
    @Select("select * from orders where id = #{id} and user_id = #{userId}")
    Orders getByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 根据订单状态统计订单数量
     * @param status
     * @return
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);


    /**
     * 根据订单状态和创建时间查询订单
     * @param status
     * @param orderTime
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time <= #{orderTime}")
    List<Orders> getOrdersByStatusAndCreateTime(Integer status, LocalDateTime orderTime);

    /**
     * 营业额统计
     *
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    @Select("select date(order_time) as day, sum(amount) as sumMoney " +
            "from orders " +
            "where order_time >= #{beginTime} and order_time <= #{endTime} and status = #{status} " +
            "group by date(order_time)")
    List<Map<String, Object>> getEveryDayTurnover(
            @Param("beginTime") LocalDateTime beginTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") Integer status
    );

    /**
     * 每天订单量统计
     * @param beginTime
     * @param endTime
     * @return
     */
    @Select("select date(order_time) as day, count(id) as order_count, " +
            "sum(case when status=5 then 1 else 0 end) as valid_order_count " +
            "from orders " +
            "where order_time >= #{beginTime} and order_time <= #{endTime} " +
            "group by date(order_time)")
    List<Map<String, Object>> getEveryDayOrderCount(@Param("beginTime") LocalDateTime beginTime,
                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 统计全部商品销量排名
     *
     * @param beginTime
     * @param endTime
     * @return
     */
    @Select("select od.name as name, sum(od.number) as number " +
            "from orders o , order_detail od  " +
            "where o.id = od.order_id " +
            "and o.status = 5 " +
            "and o.order_time >= #{beginTime} and o.order_time <= #{endTime} " +
            "group by od.name " +
            "order by number desc " )
    List<GoodsSalesDTO> getSalesTop10(@Param("beginTime") LocalDateTime beginTime,
                                      @Param("endTime") LocalDateTime endTime);
}
