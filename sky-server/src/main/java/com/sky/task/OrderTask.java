package com.sky.task;

import cn.hutool.core.util.ArrayUtil;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    //自动处理未支付的超时订单
    @Scheduled(cron = "0 * * * * *")
    public void operateOutTimeOrder(){
        log.info("自动处理超时未支付订单{}",LocalDateTime.now());

        //查询订单表中是否存在未支付且超时的订单
        LocalDateTime outTime = LocalDateTime.now().plusMinutes(-15);
        List<Orders> orderList = orderMapper.getByStatusAndOutTimeLT(Orders.PENDING_PAYMENT, outTime);
        if(ArrayUtil.isNotEmpty(orderList)){
            for (Orders orders : orderList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    //自动处理管理端未点击完成的订单
    @Scheduled(cron = "* * 1 * * ? ")
    public void operateDeliveryOrder(){
        log.info("自动处理配送中订单{}",LocalDateTime.now());
        //查询订单表中是否存在配送中的订单
        LocalDateTime outTime = LocalDateTime.now().plusMinutes(-60);
        List<Orders> orderList = orderMapper.getByStatusAndOutTimeLT(Orders.DELIVERY_IN_PROGRESS, outTime);
        if(ArrayUtil.isNotEmpty(orderList)){
            for (Orders orders : orderList) {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }
}
