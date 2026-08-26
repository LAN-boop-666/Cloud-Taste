# Day09 订单管理学习笔记

## 一、Day09 相比前八天加入了什么

前八天已经完成了用户登录、商品浏览、购物车、地址簿、提交订单和微信支付代码。Day09 不再创建新的商品模块，而是围绕已经生成的订单，补全后续处理流程。

### 用户端新增功能

1. 分页查询当前用户的历史订单。
2. 查询当前用户的一笔订单及其订单明细。
3. 用户取消待付款或待接单订单。
4. “再来一单”，把原订单商品重新放入当前用户购物车。

### 商家端新增功能

1. 按订单号、手机号、状态和下单时间搜索订单。
2. 统计待接单、待派送和派送中的订单数量。
3. 查询订单及订单明细。
4. 接单、拒单、取消订单、开始派送和完成订单。

### 下单功能的增强

提交订单前可以调用百度地图接口计算配送距离，超过 5 公里时禁止下单。由于该能力需要百度地图服务端 AK，目前默认关闭，不影响其他订单功能。

Day09 没有新建数据库表，继续使用：

```text
orders          订单主表
order_detail    订单明细表
shopping_cart   购物车表
```

## 二、代码结构发生了什么变化

```text
controller
├─ user/OrderController.java       用户订单接口继续扩展
└─ admin/OrderController.java      新增商家订单管理接口

service
├─ OrderService.java               增加 Day09 业务方法声明
└─ impl/OrderServiceImpl.java      实现订单查询和状态流转

mapper
├─ OrderMapper.java                查询订单、分页、状态统计
├─ OrderDetailMapper.java          根据订单 ID 查询明细
└─ ShoppingCartMapper.java         再来一单时批量写购物车

resources/mapper
├─ OrderMapper.xml                 动态条件查询和动态更新
└─ ShoppingCartMapper.xml          foreach 批量插入
```

调用顺序仍然是课程熟悉的三层结构：

```text
前端请求
  -> Controller 接收参数
  -> Service 处理业务规则
  -> Mapper 操作数据库
  -> Service 组装 VO
  -> Controller 返回 Result
```

## 三、Day09 需要掌握的新知识

### 1. 类级路径和方法级路径拼接

Controller 上的 `@RequestMapping` 是公共前缀，方法上的注解是具体路径。

```java
@RequestMapping("/user/order")

@GetMapping("/historyOrders")
```

最终请求路径是：

```text
/user/order/historyOrders
```

商家端同理：

```java
@RequestMapping("/admin/order")
@PutMapping("/confirm")
```

最终路径：

```text
/admin/order/confirm
```

### 2. 三种常见参数位置

#### Query 查询参数

参数写在 URL 的 `?` 后面，多个参数使用 `&` 分隔：

```text
GET /user/order/historyOrders?page=1&pageSize=10&status=5
```

对应 Controller：

```java
public Result<PageResult> page(int page, int pageSize, Integer status)
```

也可以自动封装成 DTO：

```java
public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO)
```

#### PathVariable 路径参数

参数本身是 URL 的一部分：

```java
@GetMapping("/details/{id}")
public Result<OrderVO> details(@PathVariable Long id)
```

实际请求：

```text
GET /admin/order/details/12
```

这里的 `12` 会赋值给方法参数 `id`。

#### JSON Body 请求体

适合修改操作和结构化参数：

```java
@PutMapping("/rejection")
public Result rejection(@RequestBody OrdersRejectionDTO dto)
```

请求体：

```json
{
  "id": 12,
  "rejectionReason": "商品已售罄"
}
```

请求头必须包含：

```text
Content-Type: application/json
```

### 3. 时间参数自动转换

`OrdersPageQueryDTO` 中使用：

```java
@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime beginTime;
```

因此查询参数可以写成：

```text
beginTime=2026-08-01 00:00:00
```

在浏览器或 Apifox 实际发送时，空格通常会编码成 `%20`：

```text
beginTime=2026-08-01%2000:00:00
```

### 4. 动态 SQL

商家搜索的条件不是每次都全部填写，所以 `OrderMapper.xml` 使用 `<where>` 和 `<if>`：

```xml
<where>
    <if test="number != null and number != ''">
        and number like concat('%', #{number}, '%')
    </if>
    <if test="status != null">
        and status = #{status}
    </if>
</where>
```

只有参数存在时才会拼接对应条件。`<where>` 还会自动处理开头多余的 `and`。

订单状态更新同样使用动态 SQL。Service 只给需要修改的字段赋值，XML 只更新非空字段，避免把其他列覆盖成 `null`。

### 5. PageHelper 分页

分页查询先执行：

```java
PageHelper.startPage(page, pageSize);
```

紧接着执行的第一条 MyBatis 查询会自动追加分页语句，并返回：

```text
page.getTotal()    符合条件的总记录数
page.getResult()   当前页的数据
```

最终封装为：

```java
new PageResult(page.getTotal(), records)
```

### 6. 主表和明细表组装成 VO

一条订单主表数据不包含所有商品信息。查询订单后，还要按订单 ID 查询 `order_detail`：

```java
List<OrderDetail> details = orderDetailMapper.getByOrderId(order.getId());
```

然后把主表和明细组装成 `OrderVO`：

```java
BeanUtils.copyProperties(order, orderVO);
orderVO.setOrderDetailList(details);
```

这就是 Entity 和 VO 的区别：

```text
Orders    对应订单表
OrderVO   面向前端，额外包含订单明细和菜品摘要
```

### 7. Stream 转换集合

“再来一单”需要把 `List<OrderDetail>` 转成 `List<ShoppingCart>`：

```java
List<ShoppingCart> carts = details.stream()
        .map(detail -> {
            ShoppingCart cart = new ShoppingCart();
            BeanUtils.copyProperties(detail, cart, "id");
            return cart;
        })
        .collect(Collectors.toList());
```

这里的新知识：

- `stream()`：把集合转换为流。
- `map()`：把每个订单明细转换为购物车对象。
- `collect()`：把转换结果重新收集成 List。
- `"id"`：复制属性时排除原订单明细 ID，避免错误复用主键。

### 8. foreach 批量插入

再来一单不是循环执行多条 `insert`，而是在 Mapper XML 中使用 `<foreach>` 生成一条批量 SQL：

```xml
<foreach collection="shoppingCartList" item="sc" separator=",">
    (#{sc.name}, #{sc.image}, #{sc.userId}, ...)
</foreach>
```

`separator=","` 表示每组 values 使用逗号分隔。

### 9. 订单状态机

订单不能随意跳转，正常流程是：

```text
待付款(1)
   |
   | 支付成功
   v
待接单(2) -> 已接单(3) -> 派送中(4) -> 已完成(5)
   |
   | 拒单或取消
   v
已取消(6)
```

代码会在更新前检查当前状态：

| 操作 | 操作前必须是 | 操作后变为 |
| --- | --- | --- |
| 支付成功 | 待付款 | 待接单 |
| 商家接单 | 待接单 | 已接单 |
| 商家拒单 | 待接单 | 已取消 |
| 开始派送 | 已接单 | 派送中 |
| 完成订单 | 派送中 | 已完成 |
| 用户取消 | 待付款或待接单 | 已取消 |

### 10. 事务和外部接口

取消、拒单和再来一单使用 `@Transactional`，因为它们可能包含多次数据库操作。

需要特别理解：数据库事务只能回滚数据库操作，不能撤销已经发送给微信或百度地图的 HTTP 请求。课程代码先学习基础流程，生产系统还要增加退款记录、重试和对账。

### 11. 用户订单归属校验

用户端根据订单 ID 操作时，不能只查询：

```sql
where id = #{id}
```

还要限制当前用户：

```sql
where id = #{id} and user_id = #{userId}
```

否则用户修改 URL 中的订单 ID，就可能访问别人的订单。这个校验用于详情、取消和再来一单。

## 四、请求地址到底怎么组成

后端直接调用统一使用：

```text
http://127.0.0.1:8080 + Controller 路径
```

例如：

```text
http://127.0.0.1:8080/admin/order/statistics
```

身份请求头：

```text
用户端：authentication: 用户登录返回的令牌
商家端：token: 管理员登录返回的令牌
```

Apifox 中根据接口选择：

```text
Params：Query 参数
Path Params：{id} 参数
Body -> JSON：@RequestBody 参数
Headers：token 或 authentication
```

## 五、Day09 用户端请求逐个说明

以下示例均使用后端直连地址。

### 1. 查询历史订单

```http
GET http://127.0.0.1:8080/user/order/historyOrders?page=1&pageSize=10
authentication: 用户令牌
```

按状态筛选：

```http
GET http://127.0.0.1:8080/user/order/historyOrders?page=1&pageSize=10&status=5
authentication: 用户令牌
```

参数位置：Query。

| 参数 | 必填 | 含义 |
| --- | --- | --- |
| `page` | 是 | 第几页，从 1 开始 |
| `pageSize` | 是 | 每页数量 |
| `status` | 否 | 1 待付款，2 待接单，3 已接单，4 派送中，5 已完成，6 已取消 |

### 2. 查询订单详情

```http
GET http://127.0.0.1:8080/user/order/orderDetail/12
authentication: 用户令牌
```

参数位置：Path。`12` 是订单主键 ID，不是订单号 `number`。

### 3. 用户取消订单

```http
PUT http://127.0.0.1:8080/user/order/cancel/12
authentication: 用户令牌
```

参数位置：Path，无 JSON Body。只能取消当前用户自己的待付款或待接单订单。

### 4. 再来一单

```http
POST http://127.0.0.1:8080/user/order/repetition/12
authentication: 用户令牌
```

参数位置：Path，无 JSON Body。执行后原订单明细会批量加入当前用户购物车，不会直接生成新订单。

## 六、Day09 商家端请求逐个说明

### 1. 条件搜索订单

最简单的分页请求：

```http
GET http://127.0.0.1:8080/admin/order/conditionSearch?page=1&pageSize=10
token: 管理员令牌
```

组合条件请求：

```http
GET http://127.0.0.1:8080/admin/order/conditionSearch?page=1&pageSize=10&number=1720&phone=138&status=2&beginTime=2026-08-01%2000:00:00&endTime=2026-08-31%2023:59:59
token: 管理员令牌
```

参数位置：Query，Spring 自动封装到 `OrdersPageQueryDTO`。

| 参数 | 必填 | 含义 |
| --- | --- | --- |
| `page` | 是 | 页码 |
| `pageSize` | 是 | 每页数量 |
| `number` | 否 | 订单号，支持模糊查询 |
| `phone` | 否 | 手机号，支持模糊查询 |
| `status` | 否 | 订单状态 |
| `beginTime` | 否 | 开始时间，格式 `yyyy-MM-dd HH:mm:ss` |
| `endTime` | 否 | 结束时间，格式 `yyyy-MM-dd HH:mm:ss` |

### 2. 订单状态统计

```http
GET http://127.0.0.1:8080/admin/order/statistics
token: 管理员令牌
```

无参数。返回待接单、待派送和派送中的数量。

### 3. 查询商家订单详情

```http
GET http://127.0.0.1:8080/admin/order/details/12
token: 管理员令牌
```

参数位置：Path。返回订单主表数据、订单明细和菜品摘要。

### 4. 接单

```http
PUT http://127.0.0.1:8080/admin/order/confirm
Content-Type: application/json
token: 管理员令牌

{
  "id": 12
}
```

参数位置：JSON Body。订单必须处于待接单状态 `2`，成功后变为已接单 `3`。

`OrdersConfirmDTO` 虽然还有 `status` 字段，但当前业务不会相信前端传入的状态，而是由后端固定设置为 `3`。

### 5. 拒单

```http
PUT http://127.0.0.1:8080/admin/order/rejection
Content-Type: application/json
token: 管理员令牌

{
  "id": 12,
  "rejectionReason": "商品已售罄"
}
```

参数位置：JSON Body。订单必须处于待接单状态。成功后变为已取消，并记录拒单原因和取消时间；已支付订单还会进入退款分支。

### 6. 商家取消订单

```http
PUT http://127.0.0.1:8080/admin/order/cancel
Content-Type: application/json
token: 管理员令牌

{
  "id": 12,
  "cancelReason": "门店临时打烊"
}
```

参数位置：JSON Body。成功后变为已取消，并记录取消原因和取消时间；已支付订单会进入退款分支。

### 7. 派送订单

```http
PUT http://127.0.0.1:8080/admin/order/delivery/12
token: 管理员令牌
```

参数位置：Path，无 JSON Body。订单必须处于已接单状态 `3`，成功后变为派送中 `4`。

### 8. 完成订单

```http
PUT http://127.0.0.1:8080/admin/order/complete/12
token: 管理员令牌
```

参数位置：Path，无 JSON Body。订单必须处于派送中状态 `4`，成功后变为已完成 `5`，同时写入送达时间。

## 七、配送范围请求链路

配送范围校验不是前端单独调用的新接口，而是在原有提交订单接口内部执行：

```http
POST http://127.0.0.1:8080/user/order/submit
```

启用后，后端内部请求百度地图：

```text
GET https://api.map.baidu.com/geocoding/v3
    把商家地址转换为经纬度

GET https://api.map.baidu.com/geocoding/v3
    把用户地址转换为经纬度

GET https://api.map.baidu.com/directionlite/v1/driving
    计算两个坐标之间的驾车路线距离
```

本地配置：

```yaml
sky:
  shop:
    address: ${CLOUD_TASTE_SHOP_ADDRESS:北京市海淀区上地十街10号}
  baidu:
    ak: ${BAIDU_MAP_AK:}
  delivery-range:
    enabled: ${CLOUD_TASTE_DELIVERY_RANGE_ENABLED:false}
```

没有 AK 时保持 `enabled=false`。将来启用时只设置环境变量，不把 AK 写进仓库。

## 八、建议的 Day09 验收顺序

先准备多笔不同状态的订单，然后按以下顺序测试：

```text
1. 用户查询历史订单
2. 用户查看订单详情
3. 用户再来一单，检查购物车
4. 用户取消一笔待付款订单
5. 商家搜索待接单订单
6. 商家查看详情
7. 商家接单：2 -> 3
8. 商家派送：3 -> 4
9. 商家完成：4 -> 5
10. 使用另一笔待接单订单测试拒单：2 -> 6
11. 查询统计接口，观察数量变化
```

没有微信商户号时，不要用已支付订单测试拒单或取消，因为这两个操作会调用真实退款接口。可先使用未支付订单验证非退款分支。

Day09 最重要的新知识不是某个注解，而是把分页、动态 SQL、主从表组装、批量插入、事务和状态校验组合成一个完整订单业务流程。
