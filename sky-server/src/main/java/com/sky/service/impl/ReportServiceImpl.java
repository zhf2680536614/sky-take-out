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
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private WorkspaceService workspaceService;

    @Override
    public TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end) {
        //创建集合存放范围日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //遍历时间集合，查询该时间范围内的营业额数据
        List<Double> turnoverList = new ArrayList<>();

        for (LocalDate localDate : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    @Override
    public UserReportVO getUserReport(LocalDate begin, LocalDate end) {
        //创建集合存放范围日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //创建集合用来存储用户总数量
        List<Integer> allList = new ArrayList<>();
        //创建集合用来存储新增用户数量
        List<Integer> newList = new ArrayList<>();

        for (LocalDate localDate : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);
            Map map = new HashMap();
            map.put("end", endTime);
            Integer all = userMapper.countByMap(map);
            map.put("begin", beginTime);
            Integer newUser = userMapper.countByMap(map);
            allList.add(all);
            newList.add(newUser);
        }
        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(allList, ","))
                .newUserList(StringUtils.join(newList, ","))
                .build();
    }

    @Override
    public OrderReportVO getOrdersReport(LocalDate begin, LocalDate end) {
        //创建集合存放范围日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //创建集合用来存储订单总数量
        List<Integer> allList = new ArrayList<>();
        //创建集合用来存储有效订单数量
        List<Integer> validList = new ArrayList<>();

        for (LocalDate localDate : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            Integer allOrders = orderMapper.countByMap(map);
            map.put("status", Orders.COMPLETED);
            Integer validOrders = orderMapper.countByMap(map);

            allList.add(allOrders);
            validList.add(validOrders);
        }

        //计算时间范围内的订单总数量
        Integer allCount = allList.stream().reduce(Integer::sum).get();
        //计算时间范围内的有效订单数量
        Integer validCount = validList.stream().reduce(Integer::sum).get();
        //计算有效订单率
        double orderCompletionRate = 0.0;
        if (allCount != 0) {
            orderCompletionRate = validCount.doubleValue() / allCount.doubleValue();
        }
        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(allList, ","))
                .validOrderCountList(StringUtils.join(validList, ","))
                .totalOrderCount(allCount)
                .validOrderCount(validCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> list = orderDetailMapper.getTop10(beginTime, endTime);

        List<String> names = list.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numbers = list.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO
                .builder()
                .nameList(StringUtils.join(names, ","))
                .numberList(StringUtils.join(numbers, ","))
                .build();
    }

    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //导出商业数据报表
        //1.获取商业报表所需要的数据
        LocalDate beginTime = LocalDate.now().minusDays(30);
        LocalDate endTime = LocalDate.now().minusDays(1);

        LocalDateTime begin = LocalDateTime.of(beginTime, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(endTime, LocalTime.MAX);
        BusinessDataVO businessData = workspaceService.getBusinessData(begin, end);
        //2.通过POI写入数据

        //创建输出流对象
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            if (in != null) {
                //创建excel对象
                XSSFWorkbook excel = new XSSFWorkbook(in);
                XSSFSheet sheet1 = excel.getSheet("Sheet1");
                //获取行
                XSSFRow row = sheet1.getRow(1);
                //获取列设置时间范围
                row.getCell(1).setCellValue("时间范围：" + beginTime + " 至 " + endTime);
                row = sheet1.getRow(3);
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(6).setCellValue(businessData.getNewUsers());
                row = sheet1.getRow(4);
                row.getCell(2).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getUnitPrice());

                for (int i = 0; i < 30; i++) {
                    LocalDateTime startTime = LocalDateTime.of(beginTime.plusDays(i), LocalTime.MIN);
                    LocalDateTime finalTime = LocalDateTime.of(beginTime.plusDays(i), LocalTime.MAX);
                    BusinessDataVO data = workspaceService.getBusinessData(startTime, finalTime);
                    row = sheet1.getRow(7 + i);
                    row.getCell(1).setCellValue(beginTime.plusDays(i).toString());
                    row.getCell(2).setCellValue(data.getTurnover());
                    row.getCell(3).setCellValue(data.getValidOrderCount());
                    row.getCell(4).setCellValue(data.getOrderCompletionRate());
                    row.getCell(5).setCellValue(data.getUnitPrice());
                    row.getCell(6).setCellValue(data.getNewUsers());
                }
                //3.响应给浏览器客户端下载
                ServletOutputStream out = response.getOutputStream();
                excel.write(out);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
