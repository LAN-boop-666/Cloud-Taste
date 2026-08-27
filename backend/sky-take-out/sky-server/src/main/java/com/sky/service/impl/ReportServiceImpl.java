package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end) {
        // 1.生成 [begin ~ end] 完整日期列表（包含没有订单的日期）
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate current = begin;
        while (!current.isAfter(end)) {
            dateList.add(current);
            current = current.plusDays(1);
        }
        String dateListStr = StringUtils.join(dateList, ",");

        // 2.LocalDate转LocalDateTime，补时分秒，用于Mapper查询
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        // 设置查询订单状态为“已完成”
        Integer status = Orders.COMPLETED;

        // 3.【只执行1次SQL】按天分组查询区间内有订单的每日营业额
        List<Map<String, Object>> dayDataList = orderMapper.getEveryDayTurnover(beginTime, endTime, status);

        // 4.把查询结果封装map：key=日期字符串，value=当日营业额
        Map<String, Double> turnoverMap = new HashMap<>();
        for (Map<String, Object> map : dayDataList) {
            // 1.处理日期：sql.Date → LocalDate
            java.sql.Date sqlDate = (java.sql.Date) map.get("day");
            LocalDate day = sqlDate.toLocalDate();

            // 2.处理金额：拿到BigDecimal，转为Double，同时处理null
            Object sumObj = map.get("sumMoney");
            Double money;
            if(sumObj == null){
                money = 0.0;
            }else{
                BigDecimal bigDecimal = (BigDecimal) sumObj;
                money = bigDecimal.doubleValue();
            }

            turnoverMap.put(day.toString(), money);
        }


        // 5.遍历完整日期集合，从map取数据，没有则填0
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            Double money = turnoverMap.getOrDefault(date.toString(), 0.0);
            turnoverList.add(money);
        }

        // 把List<Double>转为逗号分隔字符串
        String turnoverListStr = turnoverList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        // 6.组装VO返回
        return TurnoverReportVO.builder()
                .dateList(dateListStr)
                .turnoverList(turnoverListStr)
                .build();
    }

}
