package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    /**
     * 日期填充用的零值常量，避免循环中反复 new
     */
    private static final long[] ZERO_COUNTS = {0L, 0L};

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;


    /**
     * 营业额统计
     *
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
            if (sumObj == null) {
                money = 0.0;
            } else {
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

    /**
     * 用户统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 1.生成完整日期列表
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate current = begin;
        while (!current.isAfter(end)) {
            dateList.add(current);
            current = current.plusDays(1);
        }
        String dateListStr = StringUtils.join(dateList, ",");

        // 2.时间转换
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        //3.查询【区间内每一天新增注册用户】 来自user表！不是orders表
        List<Map<String, Object>> dayDataList = userMapper.getEveryDayNewUser(beginTime, endTime);

        //4.封装map key:日期字符串 value:当日新增用户数
        Map<String, Long> newUserMap = new HashMap<>();
        for (Map<String, Object> map : dayDataList) {
            java.sql.Date sqlDate = (java.sql.Date) map.get("day");
            LocalDate day = sqlDate.toLocalDate();
            Long newUser = (Long) map.get("new_user_count");
            // 判断newUser是否为null，避免NPE，再put到map
            newUserMap.put(day.toString(), newUser != null ? newUser : 0L);
        }

        List<Long> newUserList = new ArrayList<>();
        List<Long> totalUserList = new ArrayList<>();

        // 关键点：先查询【begin之前的总用户基数】：截止 begin前一天，一共多少注册用户
        LocalDateTime beforeBegin = LocalDateTime.of(begin.minusDays(1), LocalTime.MAX);
        // 查询截止 begin前一天的总用户数
        long totalBase = userMapper.countUserBeforeDate(beforeBegin);
        // 初始化当前总用户数为总用户基数
        long currentTotal = totalBase;

        //5.遍历完整日期，内存累加得到累计总用户
        for (LocalDate date : dateList) {
            //getOrDefault为当日新增用户数，默认值为0
            Long dayNew = newUserMap.getOrDefault(date.toString(), 0L);
            // 将当日新增用户数添加到列表
            newUserList.add(dayNew);
            // 累加当日新增用户数到当前总用户数
            currentTotal = currentTotal + dayNew; // 累加
            totalUserList.add(currentTotal);
        }

        //6.List转逗号字符串
        String newUserListStr = newUserList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String totalUserListStr = totalUserList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        //7.组装VO
        return UserReportVO.builder()
                .dateList(dateListStr)
                .newUserList(newUserListStr)
                .totalUserList(totalUserListStr)
                .build();
    }

    /**
     * 订单统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        // 1.生成完整日期列表
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate current = begin;
        while (!current.isAfter(end)) {
            dateList.add(current);
            current = current.plusDays(1);
        }
        String dateListStr = StringUtils.join(dateList, ",");

        // 2.时间转换
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        // 3.查询每天的订单总数和有效订单数
        List<Map<String, Object>> dayDataList = orderMapper.getEveryDayOrderCount(beginTime, endTime);

        // 4.封装map key:日期字符串 value:[当日订单总数,有效订单数]
        Map<String, long[]> orderCountMap = new HashMap<>();
        for (Map<String, Object> map : dayDataList) {
            // 1.处理日期：sql.Date → LocalDate
            java.sql.Date sqlDate = (java.sql.Date) map.get("day");
            LocalDate day = sqlDate.toLocalDate();
            // count/sum可能返回Long或BigDecimal，统一用Number转换
            Number orderCountNum = (Number) map.get("order_count");
            Number validOrderCountNum = (Number) map.get("valid_order_count");

            long oc = orderCountNum != null ? orderCountNum.longValue() : 0L;
            long voc = validOrderCountNum != null ? validOrderCountNum.longValue() : 0L;
            orderCountMap.put(day.toString(), new long[]{oc, voc});
        }

        // 5.单次遍历完整日期列表：补0、构建逗号字符串、累加总数
        StringJoiner orderJoiner = new StringJoiner(",");
        StringJoiner validOrderJoiner = new StringJoiner(",");
        long totalOrderCount = 0;
        long totalValidOrderCount = 0;

        for (LocalDate date : dateList) {
            long[] counts = orderCountMap.getOrDefault(date.toString(), ZERO_COUNTS);
            long oc = counts[0];
            long voc = counts[1];

            orderJoiner.add(String.valueOf(oc));
            validOrderJoiner.add(String.valueOf(voc));
            totalOrderCount += oc;
            totalValidOrderCount += voc;
        }

        // 6.计算完成率（除零保护）
        double completionRate = totalOrderCount == 0
                ? 0.0
                : (double) totalValidOrderCount / totalOrderCount;

        // 7.组装VO并返回
        return OrderReportVO.builder()
                .dateList(dateListStr)
                .orderCountList(orderJoiner.toString())
                .validOrderCountList(validOrderJoiner.toString())
                .totalOrderCount((int) totalOrderCount)
                .validOrderCount((int) totalValidOrderCount)
                .orderCompletionRate(completionRate)
                .build();
    }

    /**
     * 商品销量排名
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        // 查询全部商品销量排名
        List<GoodsSalesDTO> DataList = orderMapper.getSalesTop10(beginTime, endTime);
        // 获取商品名称列表并用逗号连接
        String nameListStr = StringUtils.join(DataList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList()), ",");

        // 获取商品数量列表并用逗号连接
        String numberListStr = StringUtils.join(DataList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList()), ",");

        return SalesTop10ReportVO.builder()
                .nameList(nameListStr)
                .numberList(numberListStr)
                .build();

    }

    /**
     * 导出数据
     *
     * @param response
     */
    @Override
    public void exportData(HttpServletResponse response) {
        //1. 查询数据库，获取最近30天的营业数据
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        log.info("开始导出运营数据报表：begin={}, end={}", begin, end);
        BusinessDataVO businessData = workspaceService.getBusinessData(beginTime, endTime);
        List<BusinessDataVO> businessDataList = workspaceService.getBusinessDataList(beginTime, endTime);

        //2. 通过POI将数据写入到Excel模板中
        InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream("template/运营数据报表模板.xlsx");
        if (inputStream == null) {
            throw new IllegalStateException("运营数据报表模板不存在");
        }

        try (InputStream is = inputStream;
             XSSFWorkbook workbook = new XSSFWorkbook(is);
             ServletOutputStream outputStream = response.getOutputStream()) {
            //设置Excel下载响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = URLEncoder.encode("运营数据统计报表.xlsx", "UTF-8")
                    .replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);

            //获取第一个Sheet
            XSSFSheet sheet = workbook.getSheetAt(0);
            //设置时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + begin + "至" + end);
            //获得第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());

            //获得第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());

            // ============填充每日明细【原版核心】============
            //模板明细起始行：第8行，下标是7
            for (int i = 0; i < businessDataList.size(); i++) {
                BusinessDataVO data = businessDataList.get(i);
                row = sheet.getRow(7 + i);
                if (row == null) {
                    row = sheet.createRow(7 + i);
                }
                for (int cellIndex = 1; cellIndex <= 6; cellIndex++) {
                    if (row.getCell(cellIndex) == null) {
                        row.createCell(cellIndex);
                    }
                }
                row.getCell(1).setCellValue(data.getDateStr());
                row.getCell(2).setCellValue(data.getTurnover());
                row.getCell(3).setCellValue(data.getValidOrderCount());
                row.getCell(4).setCellValue(data.getOrderCompletionRate());
                row.getCell(5).setCellValue(data.getUnitPrice());
                row.getCell(6).setCellValue(data.getNewUsers());
            }

            //3. 通过输出流将Excel文件下载到客户端浏览器
            workbook.write(outputStream);
            outputStream.flush();
            log.info("运营数据报表导出完成：days={}", businessDataList.size());
        } catch (IOException e) {
            log.error("运营数据报表导出失败：begin={}, end={}", begin, end, e);
            throw new RuntimeException("运营数据报表导出失败", e);
        }

    }

}


