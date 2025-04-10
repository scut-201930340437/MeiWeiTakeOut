package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.plugin.core.config.PluginRegistriesBeanDefinitionRegistrar;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

//    Field field = null;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;
    @Autowired
    private PluginRegistriesBeanDefinitionRegistrar pluginRegistriesBeanDefinitionRegistrar;

    /**
     * 统计营业额
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        TurnoverReportVO turnoverReportVO = new TurnoverReportVO();

        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        turnoverReportVO.setDateList(StringUtils.join(dateList, ","));

        // 获取日期对应的营业额数据
        List<BigDecimal> turnoverList = new ArrayList<>();
        dateList.forEach(date -> {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            map.put("status", Orders.COMPLETED);
            BigDecimal turnover = orderMapper.sumAmountByMap(map);
            turnoverList.add(turnover == null ? BigDecimal.ZERO : turnover); // 如果turnover为null，即当天没有订单，则设置为0
        });
        turnoverReportVO.setTurnoverList(StringUtils.join(turnoverList, ","));

        return turnoverReportVO;
    }

    /**
     * 统计用户数据
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        UserReportVO userReportVO = new UserReportVO();

        List<LocalDate> dateList = new ArrayList<>();

        // 获取查询begin之前的用户数，用于计算begin那天的新增用户数
        LocalDate before = begin.minusDays(1);

        // dateList
        dateList.add(before);
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<LocalDateTime> localDateTimeList = new ArrayList<>();
        for (LocalDate localDate : dateList) {
            localDateTimeList.add(LocalDateTime.of(localDate, LocalTime.MAX));
        }
        // 获取每天用户总数
        List<Map<String, Integer>> userNumMap = userMapper.sumByMap(localDateTimeList);
        List<Integer> userNumList = new ArrayList<>();
        for (Map map : userNumMap) {
            Integer userNum = (Integer) map.get("user_num");
            userNumList.add(userNum == null ? 0 : userNum);
        }

        // 获取每天新增用户数，当天数量-前一天的数量
        List<Integer> newUserList = new ArrayList<>();
        for (int i = 1; i < userNumList.size(); i++) {
            newUserList.add(userNumList.get(i) - userNumList.get(i - 1));
        }

        dateList.remove(0); // 删除before
        userNumList.remove(0); // 删除before那天用户数

        userReportVO.setDateList(StringUtils.join(dateList, ","));
        userReportVO.setTotalUserList(StringUtils.join(userNumList, ","));
        userReportVO.setNewUserList(StringUtils.join(newUserList, ","));

        return userReportVO;
    }

    /**
     * 统计订单数据
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        OrderReportVO orderReportVO = new OrderReportVO();

        HashMap<String, Integer> map = new HashMap();

        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 获取日期对应的营业额数据
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        Integer orderCount = 0;
        Integer validOrderCount = 0;
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            List<Orders> ordersList = orderMapper.getByMap(map);
            orderCountList.add(ordersList == null ? 0 : ordersList.size()); // 如果ordersList为null，即当天没有订单，则设置为0

            Integer validOrderCountEach = 0;
            if (ordersList != null && ordersList.size() > 0) {
                for (Orders orders : ordersList) {
                    if (orders.getStatus() == Orders.COMPLETED) {
                        validOrderCountEach++;
                    }
                }
            }
            validOrderCountList.add(validOrderCountEach);
        }

        orderCount = orderCountList.stream().reduce(Integer::sum).get();
        validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        orderReportVO.setDateList(StringUtils.join(dateList, ","));
        orderReportVO.setOrderCountList(StringUtils.join(orderCountList, ","));
        orderReportVO.setValidOrderCountList(StringUtils.join(validOrderCountList, ","));
        orderReportVO.setTotalOrderCount(orderCount);
        orderReportVO.setValidOrderCount(validOrderCount);
        orderReportVO.setOrderCompletionRate(orderCount > 0 ? validOrderCount * 1.0 / orderCount : 0.0);

        return orderReportVO;
    }

    /**
     * 统计销量top10
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        SalesTop10ReportVO salesTop10ReportVO = new SalesTop10ReportVO();

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        Map map = new HashMap();
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        map.put("status", Orders.COMPLETED);

        List<GoodsSalesDTO> goodsSalesDTOList = orderDetailMapper.salesByMap(map);
//        List<String> nameList = goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
//        List<Integer> numberList = goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        if (goodsSalesDTOList != null && goodsSalesDTOList.size() > 0) {
            for (GoodsSalesDTO goodsSalesDTO : goodsSalesDTOList) {
                nameList.add(goodsSalesDTO.getName());
                numberList.add(goodsSalesDTO.getNumber());
            }
        }

        salesTop10ReportVO.setNameList(StringUtils.join(nameList, ","));
        salesTop10ReportVO.setNumberList(StringUtils.join(numberList, ","));

        return salesTop10ReportVO;
    }

    /**
     * 导出运营数据报表
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        // 查询数据
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate begin = end.minusDays(30);

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        // 概览数据
        BusinessDataVO businessData = workspaceService.getBusinessData(beginTime, endTime);

        // 写入数据
        try {
            // 读入excel模板
            InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
            // 输出excel
            XSSFWorkbook excel = new XSSFWorkbook(resourceAsStream);
            XSSFSheet sheet1 = excel.getSheet("Sheet1");
            // 概览数据
            XSSFRow row = sheet1.getRow(1);
            row.getCell(1).setCellValue(begin + " - " + end); // 设置时间

            row = sheet1.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover().doubleValue()); // 营业额
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate()); // 订单完成率
            row.getCell(6).setCellValue(businessData.getNewUsers()); // 新增用户数

            row = sheet1.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount()); // 有效订单数
            row.getCell(4).setCellValue(businessData.getUnitPrice().doubleValue()); // 平均客单价

            // 明细数据
            Integer detailRowIdx = 7;
            end = end.plusDays(1);
            while (!begin.equals(end)) {
                beginTime = LocalDateTime.of(begin, LocalTime.MIN);
                endTime = LocalDateTime.of(begin, LocalTime.MAX);
                businessData = workspaceService.getBusinessData(beginTime, endTime);

                row = sheet1.getRow(detailRowIdx);

                // 写入数据
                row.getCell(1).setCellValue(begin.toString());
                row.getCell(2).setCellValue(businessData.getTurnover().doubleValue());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice().doubleValue());
                row.getCell(6).setCellValue(businessData.getNewUsers());

                // 下一天
                begin = begin.plusDays(1);
                detailRowIdx = detailRowIdx + 1;
            }

            // 写出到浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            // 关闭资源
            resourceAsStream.close();
            excel.close();
            out.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
