## 1. 项目结构

``` asciidoc
mall-order
└── src/main/java/com/mall/order
    │
    ├── interfaces                       # 入口层：HTTP / RPC / MQ
    │   ├── rest
    │   │   ├── OrderController
    │   │   ├── OrderQueryController
    │   │   └── converter
    │   │       └── OrderRequestConverter
    │   ├── rpc
    │   │   └── OrderRpcProvider
    │   ├── mq
    │   │   └── PaymentSuccessListener
    │   └── job
    │       └── OrderTimeoutJob
    │
    ├── application                      # 应用层：用例编排
    │   ├── service
    │   │   ├── OrderApplicationService          # 命令：创建/取消/支付
    │   │   ├── OrderQueryApplicationService     # 查询
    │   │   └── OrderCancelApplicationService
    │   ├── command
    │   │   ├── CreateOrderCommand
    │   │   ├── CancelOrderCommand
    │   │   └── PayOrderCommand
    │   ├── query
    │   │   ├── OrderDetailQuery
    │   │   └── OrderListQuery
    │   ├── dto
    │   │   ├── OrderDTO
    │   │   ├── OrderDetailDTO
    │   │   └── OrderItemDTO
    │   ├── assembler
    │   │   ├── OrderAssembler                   # domain <-> DTO
    │   │   └── OrderCommandAssembler            # command <-> domain
    │   └── port                                 # 应用层端口（外部上下文）
    │       ├── ProductQueryPort
    │       ├── InventoryPort
    │       ├── PromotionPort
    │       ├── PaymentPort
    │       ├── SmsNotifierPort
    │       ├── EventPublisherPort
    │       └── IdGeneratorPort
    │
    ├── domain                           # 领域层：核心业务
    │   ├── model
    │   │   ├── aggregate                        # 聚合根
    │   │   │   ├── Order
    │   │   │   └── OrderItem                    # 聚合内实体，随 Order 一起
    │   │   ├── entity                           # 独立实体（有 ID，非聚合根）
    │   │   │   ├── OrderLog
    │   │   │   └── OrderRefundRecord
    │   │   ├── valueobject                      # 值对象：无 ID，不可变
    │   │   │   ├── OrderId
    │   │   │   ├── OrderNo
    │   │   │   ├── OrderStatus
    │   │   │   ├── OrderAmount
    │   │   │   ├── Address
    │   │   │   ├── Receiver
    │   │   │   ├── SkuSnapshot
    │   │   │   ├── Quantity
    │   │   │   └── CancelReason
    │   │   └── enums
    │   │       ├── OrderStatusEnum
    │   │       └── PayChannelEnum
    │   │
    │   ├── service                              # 领域服务：跨聚合规则
    │   │   ├── OrderDomainService
    │   │   ├── OrderPriceDomainService
    │   │   └── OrderCancelDomainService
    │   │
    │   ├── repository                           # 聚合仓储接口
    │   │   ├── OrderRepository
    │   │   └── OrderLogRepository
    │   │
    │   ├── port                                 # 领域端口：领域需要的能力
    │   │   ├── ExchangeRatePort
    │   │   ├── TaxCalculatorPort
    │   │   └── OrderRulePort
    │   │
    │   └── event                                # 领域事件
    │       ├── OrderCreatedEvent
    │       ├── OrderPaidEvent
    │       ├── OrderCancelledEvent
    │       └── OrderCompletedEvent
    │
    └── infrastructure                   # 基础设施：实现所有 Port
        ├── persistence
        │   ├── OrderRepositoryImpl              # 实现 domain.repository
        │   ├── OrderLogRepositoryImpl
        │   ├── po
        │   │   ├── OrderPO
        │   │   ├── OrderItemPO
        │   │   └── OrderLogPO
        │   ├── mapper
        │   │   ├── OrderMapper
        │   │   └── OrderItemMapper
        │   └── converter
        │       └── OrderPOConverter             # PO <-> domain
        ├── acl                                  # 防腐层：调外部上下文
        │   ├── ProductQueryPortImpl             # 实现 application.port
        │   ├── InventoryPortImpl
        │   ├── PromotionPortImpl
        │   ├── PaymentPortImpl
        │   └── client
        │       ├── ProductClient
        │       └── InventoryClient
        ├── notify
        │   └── SmsNotifierAdapter               # 实现 application.port
        ├── mq
        │   ├── EventPublisherAdapter            # 实现 application.port
        │   └── OrderEventProducer
        ├── cache
        │   └── OrderCacheAdapter
        ├── id
        │   └── SnowflakeIdGenerator             # 实现 application.port
        └── config
            ├── OrderDomainConfig
            └── OrderClientConfig

```
## 2. domain 内部细分详解
```asciidoc
domain
├── model
│   ├── aggregate          # 聚合根：一致性边界，唯一对外入口
│   ├── entity             # 实体：有 ID，生命周期独立但不做聚合根
│   ├── valueobject        # 值对象：无 ID，不可变，用值相等
│   └── enums              # 领域枚举
│
├── service                # 领域服务：跨聚合的领域规则
├── repository             # 聚合仓储接口
├── port                   # 领域端口：领域需要的外部能力
└── event                  # 领域事件
```

### 2.1 聚合根（Aggregate）
```asciidoc
特征：
  - 有全局唯一 ID
  - 是一致性边界，事务一次只改一个聚合
  - 外部只能通过聚合根访问内部实体
  - 内部实体不单独建 Repository
```
```java
package com.mall.order.domain.model.aggregate;

public class Order {                       // 聚合根
    private OrderId id;
    private OrderNo orderNo;
    private UserId userId;
    private OrderStatus status;
    private List<OrderItem> items;         // 聚合内实体
    private Address address;
    private OrderAmount amount;
    private List<OrderLog> logs;           // 聚合内实体

    // 工厂方法
    public static Order create(CreateOrderCommand cmd) { /*...*/ }

    // 业务行为
    public void pay(PayChannel channel) { /*...*/ }
    public void cancel(CancelReason reason) { /*...*/ }
    public void complete() { /*...*/ }

    // 领域事件
    public List<DomainEvent> pullEvents() { /*...*/ }
}
```
```java
package com.mall.order.domain.model.aggregate;

public class OrderItem {                   // 聚合内实体，不是聚合根
    private OrderItemId id;                // 局部 ID
    private SkuId skuId;
    private SkuSnapshot snapshot;
    private Quantity quantity;
    private Money price;
}
```
### 2.2 实体（Entity）
```asciidoc
特征：
  - 有 ID，但不需要作为聚合根
  - 生命周期由某个聚合管理，或独立但简单
  - 例如：日志、流水、明细记录
```
```java
package com.mall.order.domain.model.entity;

public class OrderLog {
    private OrderLogId id;
    private OrderId orderId;
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private String operator;
    private LocalDateTime createdAt;
}
```
### 2.3 值对象（Value Object）
```asciidoc
特征：
  - 无 ID，不可变
  - 用值相等
  - 自带校验，构造即合法
  - 领域概念的最小单位
```
```java
package com.mall.order.domain.model.valueobject;

public final class OrderId {
    private final String value;

    private OrderId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OrderId 不能为空");
        }
        this.value = value;
    }

    public static OrderId of(String value) { return new OrderId(value); }
    public static OrderId generate(IdGenerator gen) { return new OrderId(gen.next()); }
    public String value() { return value; }

    @Override
    public boolean equals(Object o) { /*...*/ }
    @Override
    public int hashCode() { /*...*/ }
}
```
```java
package com.mall.order.domain.model.valueobject;

public final class OrderAmount {
    private final Money total;
    private final Money discount;
    private final Money payable;

    private OrderAmount(Money total, Money discount, Money payable) {
        // 校验：payable = total - discount >= 0
        this.total = total;
        this.discount = discount;
        this.payable = payable;
    }

    public static OrderAmount of(Money total, Money discount) {
        Money payable = total.subtract(discount);
        if (payable.isNegative()) throw new IllegalArgumentException("应付金额不能为负");
        return new OrderAmount(total, discount, payable);
    }
}
```
```java
package com.mall.order.domain.model.valueobject;

public final class OrderAmount {
    private final Money total;
    private final Money discount;
    private final Money payable;

    private OrderAmount(Money total, Money discount, Money payable) {
        // 校验：payable = total - discount >= 0
        this.total = total;
        this.discount = discount;
        this.payable = payable;
    }

    public static OrderAmount of(Money total, Money discount) {
        Money payable = total.subtract(discount);
        if (payable.isNegative()) throw new IllegalArgumentException("应付金额不能为负");
        return new OrderAmount(total, discount, payable);
    }
}
```
```java
package com.mall.order.domain.model.valueobject;

public final class Address {
    private final String province;
    private final String city;
    private final String district;
    private final String detail;
    private final String receiverName;
    private final String receiverPhone;

    // 构造校验 + 不可变
}
```
### 2.4 enums （领域枚举）
```java
package com.mall.order.domain.model.enums;

public enum OrderStatusEnum {
    CREATED, PENDING_PAY, PAID, SHIPPED, COMPLETED, CANCELLED
}
```
### 2.5 领域服务（Domain Service）
```asciidoc
package com.mall.order.domain.service;

public class OrderPriceDomainService {

    private final ExchangeRatePort exchangeRatePort;   // domain.port
    private final TaxCalculatorPort taxPort;           // domain.port

    public OrderAmount calculate(Order order) {
        // 跨聚合/跨概念的价格规则
    }
}
```
### 2.6 聚合仓储接口（Repository）
```java
package com.mall.order.domain.repository;

public interface OrderRepository {
    Order findById(OrderId id);
    Order findByOrderNo(OrderNo no);
    void save(Order order);
    void remove(Order order);
}
```
### 2.7 领域端口（Port）
```java
package com.mall.order.domain.port;

public interface ExchangeRatePort {
    ExchangeRate getRate(Currency from, Currency to);
}
```
```java
package com.mall.order.domain.port;

public interface TaxCalculatorPort {
    Money calculateTax(Money amount, Address address);
}
```
### 2.8 领域事件（Domain Event）
```java
package com.mall.order.domain.event;

public class OrderCreatedEvent implements DomainEvent {
    private final OrderId orderId;
    private final UserId userId;
    private final OrderAmount amount;
    private final LocalDateTime occurredAt;
}
```

## 3.application 内部细分详解

```asciidoc
application
├── service        # 应用服务：用例编排
├── command        # 命令对象（写）
├── query          # 查询对象（读）
├── dto            # 对外数据传输对象
├── assembler      # domain <-> DTO 转换
└── port           # 应用端口：外部上下文能力
```
### 3.1 应用服务（Application Service）
```java
package com.mall.order.application.service;

public class OrderApplicationService {

    private final OrderRepository orderRepository;          // domain.repository
    private final OrderDomainService orderDomainService;    // domain.service
    private final ProductQueryPort productQueryPort;        // application.port
    private final InventoryPort inventoryPort;              // application.port
    private final PromotionPort promotionPort;              // application.port
    private final EventPublisherPort eventPublisher;        // application.port
    private final OrderAssembler assembler;

    @Transactional
    public OrderDTO createOrder(CreateOrderCommand cmd) {
        // 1. 幂等
        // 2. 调外部端口加载数据
        // 3. 创建聚合
        // 4. 领域服务校验
        // 5. 保存
        // 6. 发事件
        // 7. 组装 DTO
    }
}
```
### 3.2 命令对象（Command）
```java
package com.mall.order.application.command;
public class CreateOrderCommand { /*...*/ }
```
### 3.3 查询对象（Query）
```java
package com.mall.order.application.query;
public class OrderDetailQuery {/*...*/}
```
### 3.4 DTO（Data Transfer Object）
```java
package com.mall.order.application.dto;
public class OrderDTO { /*...*/ }
```
### 3.5 Assembler（组装器）
```java
package com.mall.order.application.assembler;

public class OrderAssembler {
    public static OrderDTO toDTO(Order order) { /*...*/ }
    public static Order toDomain(CreateOrderCommand cmd) { /*...*/ }
}
```
### 3.6 应用端口（Port）
```java
package com.mall.order.application.port;

public interface ProductQueryPort {
    List<SkuSnapshot> getSkuSnapshots(List<SkuId> skuIds);
}
```
```java
package com.mall.order.application.port;

public interface InventoryPort {
    List<InventoryLock> lock(List<InventoryLockRequest> requests);
    void release(List<InventoryLock> locks);
}
```
```java
package com.mall.order.application.port;

public interface SmsNotifierPort {
    void sendOrderCreatedSms(String phone, String orderNo);
}
```

## 4.interfaces 内部细分详解

```asciidoc
interfaces
├── rest           # HTTP
├── rpc            # 对内 RPC Provider
├── mq             # 消息监听
├── job            # 定时任务
└── converter      # 请求/响应 <-> command/dto
```
```java
package com.mall.order.interfaces.rest;

@RestController
public class OrderController {

    private final OrderApplicationService orderAppService;

    @PostMapping("/orders")
    public OrderResponse create(@RequestBody CreateOrderRequest req) {
        CreateOrderCommand cmd = OrderRequestConverter.toCommand(req);
        OrderDTO dto = orderAppService.createOrder(cmd);
        return OrderResponse.from(dto);
    }
}
```
## 5.infrastructure 内部细分详解

```asciidoc
infrastructure
├── persistence    # 实现 domain.repository
│   ├── *RepositoryImpl
│   ├── po         # 数据库持久化对象
│   ├── mapper     # MyBatis Mapper
│   └── converter  # PO <-> domain
├── acl            # 实现 application.port，调外部上下文
│   ├── *PortImpl
│   └── client
├── notify         # 实现 application.port
├── mq             # 实现 application.port + 消息生产/消费
├── cache          # 实现 application.port
├── id             # 实现 application.port
├── external       # 实现 domain.port（如汇率、税费）
│   └── ExchangeRateApiAdapter
└── config         # Spring 配置
```
注意
```asciidoc
domain.port 的实现     -> infrastructure/external（如 ExchangeRateApiAdapter）
application.port 的实现 -> infrastructure/acl、notify、mq、cache、id
domain.repository 的实现 -> infrastructure/persistence
```
## 6. Port 归属速查

```asciidoc
domain/
├── repository        # 聚合仓储，领域层定义，领域层可调
│   └── OrderRepository
└── port              # 领域能力，领域层定义，领域层可调
    ├── ExchangeRatePort
    └── TaxCalculatorPort

application/
└── port              # 应用能力，应用层定义，应用层调
    ├── ProductQueryPort
    ├── InventoryPort
    ├── PromotionPort
    ├── PaymentPort
    ├── SmsNotifierPort
    ├── EventPublisherPort
    └── IdGeneratorPort
```
```asciidoc
聚合的持久化          -> domain/repository
领域规则需要的能力    -> domain/port
外部上下文的数据      -> application/port
通知/事件/缓存/ID     -> application/port
```
## 7. 领域事件与消息

```asciidoc
domain/event       # 领域事件，领域层定义
  OrderCreatedEvent

application/port   # 发布能力，应用层定义
  EventPublisherPort

infrastructure/mq  # 实现
  EventPublisherAdapter
  OrderEventProducer

interfaces/mq      # 消费入口
  PaymentSuccessListener
```
```asciidoc
聚合内注册事件 -> ApplicationService 保存聚合 -> 调 EventPublisherPort -> Adapter 投递 MQ
```
## 8. 完整依赖方向
```asciidoc
interfaces
    │
    ▼
application ──────► application/port
    │                    ▲
    │                    │
    ▼                    │
domain ──────────► domain/repository
    │                    ▲
    │                    │
    ▼                    │
domain/port ─────────────┘
                         │
                         │
infrastructure ──────────┘
  （实现所有 Port）
```
规则
```asciidoc
interfaces -> application
application -> domain
application -> application/port
domain -> domain/repository
domain -> domain/port
infrastructure -> 实现以上所有 Port

禁止：
domain -X-> application
domain -X-> infrastructure
application -X-> infrastructure
```
## 9. 综合
```asciidoc
顶层：按限界上下文分模块（order/product/inventory/...）
每个上下文内：
  interfaces  -> 入口（rest/rpc/mq/job）
  application -> 用例（service/command/query/dto/assembler/port）
  domain      -> 核心（model{aggregate,entity,valueobject,enums}/service/repository/port/event）
  infrastructure -> 实现（persistence/acl/notify/mq/cache/id/external/config）

domain.model 细分：
  aggregate   -> 聚合根，一致性边界
  entity      -> 有 ID 的独立实体
  valueobject -> 无 ID 不可变的值对象
  enums       -> 领域枚举

Port 归属：
  domain/repository  聚合持久化
  domain/port        领域能力（ExchangeRatePort、TaxPort）
  application/port   外部上下文 + 应用能力（ProductQueryPort、SmsPort）

依赖方向永远指向内：
  interfaces -> application -> domain
  infrastructure 实现所有 Port，不被内层依赖
```

