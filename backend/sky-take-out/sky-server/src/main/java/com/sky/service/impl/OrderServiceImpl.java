package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.entity.User;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单业务实现类
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.baidu.ak:}")
    private String baiduAk;
    @Value("${sky.delivery-range.enabled:false}")
    private boolean deliveryRangeEnabled;

    /**
     * 提交订单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        log.info("开始处理提交订单：addressBookId={}", ordersSubmitDTO.getAddressBookId());
        //处理各种业务异常(地址簿为空、购物车数据为空)
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        Long currentId = BaseContext.getCurrentId();
        if (addressBook == null || !currentId.equals(addressBook.getUserId())) {
            //抛出地址簿为空的业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        checkOutOfRange(addressBook.getDetail());

        //获取当前登录用户的id
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(currentId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        log.info("查询购物车完成：userId={}, itemCount={}", currentId, list == null ? 0 : list.size());
        if (list == null || list.size() == 0) {
            //抛出购物车为空的业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //向订单表插入1条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(currentId);
        orders.setAddress(addressBook.getDetail());
        orders.setUserName(addressBook.getConsignee());
        BigDecimal amount = list.stream()
                .map(item -> item.getAmount().multiply(BigDecimal.valueOf(item.getNumber())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (ordersSubmitDTO.getPackAmount() != null && ordersSubmitDTO.getPackAmount() < 0) {
            throw new OrderBusinessException("打包费不能为负数");
        }
        if (ordersSubmitDTO.getPackAmount() != null) {
            amount = amount.add(BigDecimal.valueOf(ordersSubmitDTO.getPackAmount()));
        }
        orders.setAmount(amount);

        orderMapper.insert(orders);
        log.info("订单主表保存成功：orderId={}, orderNumber={}, amount={}",
                orders.getId(), orders.getNumber(), orders.getAmount());


        List<OrderDetail> orderDetails = new ArrayList<>();
        //向订单明细表插入n条数据
        for (ShoppingCart cart : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);
        //清空当前用户的购物车数据
        shoppingCartMapper.cleanShoppingCart(currentId);
        log.info("提交订单完成，购物车已清空：orderId={}", orders.getId());
        //封装VO放回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     * @throws Exception
     */
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("开始处理订单支付：orderNumber={}", ordersPaymentDTO.getOrderNumber());
        //获取当前登录用户的openid
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);
        //调用微信支付接口，生成小程序调起支付所需参数
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(),
                new BigDecimal(0.01),
                "苍穹外卖订单",
                user.getOpenid()
        );
        //微信支付返回ORDERPAID表示该订单已经支付
        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            log.warn("订单已支付，无需重复支付：orderNumber={}", ordersPaymentDTO.getOrderNumber());
            throw new OrderBusinessException("该订单已支付");
        }
        //封装支付参数并返回给小程序
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        log.info("订单支付预下单完成：orderNumber={}", ordersPaymentDTO.getOrderNumber());
        return vo;
    }

    /**
     * 支付成功，修改订单状态并发送来单提醒
     *
     * @param outTradeNo
     */
    @Override
    @Transactional
    public void paySuccess(String outTradeNo) {
        log.info("收到支付成功通知：orderNumber={}", outTradeNo);
        //根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);
        //将订单修改为已支付、待接单状态
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
        log.info("订单支付状态更新完成：orderId={}, status={}", ordersDB.getId(), Orders.TO_BE_CONFIRMED);

        //通过websocket向客户端浏览器推送消息 type orderId content
        Map map = new HashMap();
        map.put("type",1);//1表示来单提醒 2表示客户催单
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号:"+outTradeNo);

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
        log.info("来单提醒发送完成：orderId={}", ordersDB.getId());
    }

    @Override
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        //根据当前登录用户和订单状态分页查询订单
        PageHelper.startPage(pageNum, pageSize);
        OrdersPageQueryDTO query = new OrdersPageQueryDTO();
        query.setUserId(BaseContext.getCurrentId());
        query.setStatus(status);
        Page<Orders> page = orderMapper.pageQuery(query);
        log.info("用户历史订单查询完成：userId={}, status={}, total={}", query.getUserId(), status, page.getTotal());
        //查询订单明细并组装用户端返回对象
        List<OrderVO> records = page.getResult().stream().map(this::toOrderVO).collect(Collectors.toList());
        return new PageResult(page.getTotal(), records);
    }

    @Override
    public OrderVO detailsForUser(Long id) {
        log.info("开始查询用户订单详情：orderId={}, userId={}", id, BaseContext.getCurrentId());
        //按订单id和当前用户id查询，防止用户访问其他人的订单
        Orders order = orderMapper.getByIdAndUserId(id, BaseContext.getCurrentId());
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return toOrderVO(order);
    }

    @Override
    public OrderVO details(Long id) {
        log.info("开始查询商家订单详情：orderId={}", id);
        //商家端查询订单主表数据
        Orders order = orderMapper.getById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return toOrderVO(order);
    }

    @Override
    @Transactional
    public void userCancelById(Long id) throws Exception {
        //查询当前用户自己的订单
        Orders order = orderMapper.getByIdAndUserId(id, BaseContext.getCurrentId());
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (order.getStatus() > Orders.TO_BE_CONFIRMED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //待接单且已支付的订单需要先申请退款
        Orders update = new Orders();
        update.setId(order.getId());
        if (Orders.TO_BE_CONFIRMED.equals(order.getStatus()) && Orders.PAID.equals(order.getPayStatus())) {
            refund(order);
            update.setPayStatus(Orders.REFUND);
        }
        update.setStatus(Orders.CANCELLED);
        update.setCancelReason("用户取消");
        update.setCancelTime(LocalDateTime.now());
        orderMapper.update(update);
        log.info("用户取消订单完成：orderId={}", order.getId());
    }

    @Override
    @Transactional
    public void repetition(Long id) {
        //查询原订单明细，并复制到当前用户购物车
        Orders order = orderMapper.getByIdAndUserId(id, BaseContext.getCurrentId());
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        List<OrderDetail> details = orderDetailMapper.getByOrderId(order.getId());
        if (details == null || details.isEmpty()) {
            throw new OrderBusinessException("订单明细为空");
        }
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> carts = details.stream().map(detail -> {
            ShoppingCart cart = new ShoppingCart();
            BeanUtils.copyProperties(detail, cart, "id");
            cart.setUserId(userId);
            cart.setCreateTime(LocalDateTime.now());
            return cart;
        }).collect(Collectors.toList());
        shoppingCartMapper.insertBatch(carts);
        log.info("再来一单完成：orderId={}, itemCount={}", order.getId(), carts.size());
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO query) {
        //根据订单号、手机号、状态和时间条件分页查询
        PageHelper.startPage(query.getPage(), query.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(query);
        log.info("商家订单搜索完成：total={}", page.getTotal());
        //组装订单明细和菜品展示字符串
        List<OrderVO> records = page.getResult().stream().map(this::toOrderVO).collect(Collectors.toList());
        return new PageResult(page.getTotal(), records);
    }

    @Override
    public OrderStatisticsVO statistics() {
        //分别统计待接单、待派送和派送中的订单数量
        OrderStatisticsVO statistics = new OrderStatisticsVO();
        statistics.setToBeConfirmed(orderMapper.countStatus(Orders.TO_BE_CONFIRMED));
        statistics.setConfirmed(orderMapper.countStatus(Orders.CONFIRMED));
        statistics.setDeliveryInProgress(orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS));
        log.info("订单状态统计：toBeConfirmed={}, confirmed={}, deliveryInProgress={}",
                statistics.getToBeConfirmed(), statistics.getConfirmed(), statistics.getDeliveryInProgress());
        return statistics;
    }

    @Override
    public void confirm(OrdersConfirmDTO dto) {
        log.info("开始接单：orderId={}", dto.getId());
        //只有待接单订单可以接单
        Orders order = getOrder(dto.getId());
        if (!Orders.TO_BE_CONFIRMED.equals(order.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders update = new Orders();
        update.setId(order.getId());
        update.setStatus(Orders.CONFIRMED);
        orderMapper.update(update);
        log.info("订单已接单：orderId={}", order.getId());
    }

    @Override
    @Transactional
    public void rejection(OrdersRejectionDTO dto) throws Exception {
        log.info("开始拒单：orderId={}", dto.getId());
        //只有待接单订单可以拒单
        Orders order = getOrder(dto.getId());
        if (!Orders.TO_BE_CONFIRMED.equals(order.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders update = new Orders();
        update.setId(order.getId());
        if (Orders.PAID.equals(order.getPayStatus())) {
            refund(order);
            update.setPayStatus(Orders.REFUND);
        }
        update.setStatus(Orders.CANCELLED);
        update.setRejectionReason(dto.getRejectionReason());
        update.setCancelTime(LocalDateTime.now());
        orderMapper.update(update);
        log.info("订单已拒单：orderId={}", order.getId());
    }

    @Override
    @Transactional
    public void cancel(OrdersCancelDTO dto) throws Exception {
        log.info("开始商家取消订单：orderId={}", dto.getId());
        //商家取消订单时，已支付订单需要申请退款
        Orders order = getOrder(dto.getId());
        Orders update = new Orders();
        update.setId(order.getId());
        if (Orders.PAID.equals(order.getPayStatus())) {
            refund(order);
            update.setPayStatus(Orders.REFUND);
        }
        update.setStatus(Orders.CANCELLED);
        update.setCancelReason(dto.getCancelReason());
        update.setCancelTime(LocalDateTime.now());
        orderMapper.update(update);
        log.info("商家取消订单完成：orderId={}", order.getId());
    }

    @Override
    public void delivery(Long id) {
        log.info("开始派送订单：orderId={}", id);
        //只有已接单订单可以进入派送中状态
        Orders order = getOrder(id);
        if (!Orders.CONFIRMED.equals(order.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders update = new Orders();
        update.setId(id);
        update.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(update);
        log.info("订单进入派送中：orderId={}", order.getId());
    }

    @Override
    public void complete(Long id) {
        log.info("开始完成订单：orderId={}", id);
        //只有派送中订单可以完成
        Orders order = getOrder(id);
        if (!Orders.DELIVERY_IN_PROGRESS.equals(order.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders update = new Orders();
        update.setId(id);
        update.setStatus(Orders.COMPLETED);
        update.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(update);
        log.info("订单已完成：orderId={}", order.getId());
    }

    private Orders getOrder(Long id) {
        //根据订单id查询订单，不存在时统一抛出业务异常
        Orders order = orderMapper.getById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return order;
    }

    private OrderVO toOrderVO(Orders order) {
        //将订单主表和订单明细组装成前端需要的 OrderVO
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);
        List<OrderDetail> details = orderDetailMapper.getByOrderId(order.getId());
        orderVO.setOrderDetailList(details);
        if (details != null) {
            orderVO.setOrderDishes(details.stream()
                    .map(detail -> detail.getName() + "*" + detail.getNumber())
                    .collect(Collectors.joining("; ")));
        }
        return orderVO;
    }

    private void refund(Orders order) throws Exception {
        //课程阶段沿用微信退款接口，真实退款需要商户号配置
        BigDecimal amount = order.getAmount() == null ? new BigDecimal("0.01") : order.getAmount();
        String result = weChatPayUtil.refund(order.getNumber(), order.getNumber(), amount, amount);
        log.info("申请退款：{}", result);
    }

    /**
     * 百度地图校验是课程要求的外部能力。没有启用或没有 AK 时跳过，避免本地学习环境被第三方配置阻断。
     */
    private void checkOutOfRange(String address) {
        if (!deliveryRangeEnabled || !StringUtils.hasText(baiduAk)) {
            log.info("配送范围校验未启用，跳过百度地图检查");
            return;
        }
        if (!StringUtils.hasText(address)) {
            throw new OrderBusinessException("收货地址不能为空");
        }
        Map<String, String> params = new HashMap<>();
        params.put("address", shopAddress);
        params.put("output", "json");
        params.put("ak", baiduAk);
        JSONObject shopResult = parseMapResponse(HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", params), "店铺地址解析失败");
        JSONObject shopLocation = shopResult.getJSONObject("result").getJSONObject("location");
        String shopCoordinate = shopLocation.getString("lat") + "," + shopLocation.getString("lng");

        params.put("address", address);
        JSONObject userResult = parseMapResponse(HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", params), "收货地址解析失败");
        JSONObject userLocation = userResult.getJSONObject("result").getJSONObject("location");
        String userCoordinate = userLocation.getString("lat") + "," + userLocation.getString("lng");

        params.remove("address");
        params.put("origin", shopCoordinate);
        params.put("destination", userCoordinate);
        params.put("steps_info", "0");
        JSONObject routeResult = parseMapResponse(HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", params), "配送路线规划失败");
        JSONArray routes = routeResult.getJSONObject("result").getJSONArray("routes");
        if (routes == null || routes.isEmpty() || routes.getJSONObject(0).getInteger("distance") == null) {
            throw new OrderBusinessException("配送路线规划失败");
        }
        if (routes.getJSONObject(0).getInteger("distance") > 5000) {
            throw new OrderBusinessException("超出配送范围");
        }
    }

    private JSONObject parseMapResponse(String response, String message) {
        if (!StringUtils.hasText(response)) {
            throw new OrderBusinessException(message);
        }
        JSONObject result = JSONObject.parseObject(response);
        if (!"0".equals(result.getString("status")) || result.getJSONObject("result") == null) {
            throw new OrderBusinessException(message);
        }
        return result;
    }
}
