package com.sky.mapper;


import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    /**
     * 批量插入订单明细数据
     * @param orderDetails
     */
    void insertBatch(List<OrderDetail> orderDetails);

    /**
     * 根据订单id查询订单明细
     * @param orderId
     * @return
     */
    @Select("select * from order_detail where order_id = #{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);

    /**
     * 根据订单id批量查询订单明细，减少分页查询时的N+1次数据库访问
     * @param orderIds
     * @return
     */
    @Select({
            "<script>",
            "select * from order_detail where order_id in",
            "<foreach collection='orderIds' item='orderId' open='(' separator=',' close=')'>",
            "#{orderId}",
            "</foreach>",
            "</script>"
    })
    List<OrderDetail> getByOrderIds(@Param("orderIds") List<Long> orderIds);
}
