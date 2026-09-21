```asciidoc
com-ovelin
├── mall-common                     # 共享内核：Money、Currency、UserId 等通用值对象
├── mall-bom                        # 依赖版本管理
│
├── mall-user                       # 用户上下文
├── mall-product                    # 商品上下文
├── mall-inventory                  # 库存上下文
├── mall-cart                       # 购物车上下文
├── mall-order                      # 订单上下文
├── mall-payment                    # 支付上下文
├── mall-promotion                  # 营销上下文
├── mall-fulfillment                # 履约上下文
│
├── mall-user-api                   # 各上下文对外契约
├── mall-product-api
├── mall-inventory-api
├── mall-order-api
├── mall-payment-api
├── mall-promotion-api
└── mall-fulfillment-api
```
```asciidoc
mall-order  ->  mall-order-api
mall-order  ->  mall-product-api      (调商品上下文)
mall-order  ->  mall-inventory-api    (调库存上下文)
mall-order  ->  mall-promotion-api    (调营销上下文)

mall-order  -X-> mall-product         (禁止依赖实现)
mall-product -X-> mall-order          (禁止反向依赖)
```