```markdown
# saas门店品操作通知 (saas.goods.store)

**消息Topic**: `saas.goods.store`  
**鉴权**: 不需要用户授权 L1  
**说明**: 当用户在SaaS页面操作门店品后，平台会通过应用设置的下行推送地址将操作类型推送给商家。

---

## 推送消息

### 系统级输入参数

| 名称 | 类型 | 是否必须 | 描述 |
|:---|:---|:---|:---|
| `cmd` | String | 是 | 接口cmd |
| `version` | String | 是 | 版本,默认3 |
| `timestamp` | Long | 是 | 时间戳 |
| `ticket` | String | 是 | 请求流水号 |
| `source` | String | 是 | 填写APPID |
| `sign` | String | 是 | 签名,md5 |
| `encrypt` | String | 否 | 是否加密,如AES,默认为空 |

### 应用级输入参数

| 名称 | 字段类型 | 是否必须 | 字段描述 |
|:---|:---|:---|:---|
| `app_id` | String | 是 | 商家开放唯一编码 |
| `merchant_code` | String | 是 | 商家编码 |
| `spu_code` | String | 否 | 商品编码 |
| `sku_code` | String | 是 | 规格编码 |
| `sub_sku_code` | String | 否 | 规格店内码 |
| `goods_level` | String | 是 | 商品类型 |
| `operation_source` | String | 否 | 操作来源 |
| `operation_type` | String | 是 | 操作类型 |
| `store_code` | String | 否 | AX门店编码 |
| `erp_store_code` | String | 否 | 外部门店编码 |
| `consignment_outer_sku_code` | String | 否 | 寄售外部规格编码 |
| `consignment_type` | String | 否 | 寄售品类型 |
| `owner_code` | String | 否 | 货主编码 |
| `owner_name` | String | 否 | 货主名称 |

### 消息示例

```json
{
    "ticket": "026B5CDD-B756-47FD-9DE4-617A431123CB",
    "encrypt": "",
    "sign": "9CC8626E6E1EF21C8160AA8FEF33ADFF",
    "cmd": "saas.order.create",
    "source": "65336",
    "body": {
        "appId": "42267022123",
        "merchant_code": "2158084182923231123",
        "spu_code": "@@123",
        "sku_code": "123",
        "sub_sku_code": "123",
        "goods_level": "MasterGoods/StoreGoods",
        "operation_source": "PC",
        "operation_type": "CREATE/UPDATE/DELETE",
        "store_code": "axStoreCode",
        "erp_store_code": "erpStoreCode"
    },
    "version": 3,
    "timestamp": 1629190142
}
```

---

## 商家响应

### 响应参数

| 字段名 | 字段类型 | 备注 |
|:---|:---|:---|
| `errno` | Number | 状态码：成功返回0，失败返回其他 |
| `error` | String | 状态说明：成功返回success，失败返回其他 |

### 响应示例

```json
HTTP/1.1 200 OK
{
    "body": {
        "errno": 0,
        "error": "success"
    },
    "cmd": "xxx.xxx.xxx",
    "sign": "51BAA29E9CE298241F52985864D23165",
    "source": "65400",
    "ticket": "FEBCA99A-967D-EBDC-8588-F530B3E235E7",
    "timestamp": 1452686921,
    "version": 3
}
```

---

# saas批量查询门店商品

**接口标识**: `me.ele.retail:saas.goods.store.query.batch-3`  
**鉴权**: 不需要授权 L1  
**说明**: 分页查询门店商品返回商品列表

---

## 系统级输入参数

| 名称 | 类型 | 是否必须 | 描述 |
|:---|:---|:---|:---|
| `cmd` | String | 是 | 接口cmd |
| `version` | String | 是 | 版本,默认3 |
| `timestamp` | Long | 是 | 时间戳 |
| `ticket` | String | 是 | 请求流水号 |
| `source` | String | 是 | 填写APPID |
| `sign` | String | 是 | 签名,md5 |
| `encrypt` | String | 否 | 是否加密,如AES,默认为空 |
| `access_token` | String | 否 | 访问令牌,访问用户隐私数据时的唯一权限标识。如果API不需要授权则可以不带入此参数 |

## 应用级输入参数

| 名称 | 类型 | 是否必须 | 描述 | 示例值 |
|:---|:---|:---|:---|:---|
| `body` | message:me.ele.newretail.tc.open.erp.client.request.goods.StoreGoodsQueryRequest | 是 | 请求参数 | {} |
| `merchant_code` | String | 是 | 商家编码 | test123 |
| `erp_store_code` | String | 是 | erp门店编码（erp接入场景必填） | store123 |
| `sku_code_list` | String[] | 否 | 商家编码列表（不填则返回全部门店商品） | ["1234","5678"] |
| `page_size` | Integer | 是 | 分页大小 | 20以下 |
| `page_no` | Integer | 是 | 页码 | 从1开始 |

## 返回结果

### 顶层结构

| 名称 | 类型 | 描述 | 示例值 |
|:---|:---|:---|:---|
| `errno` | String | 返回错误码 | 0 |
| `error` | String | 返回错误信息 | success |
| `data` | message:me.ele.newretail.tc.open.erp.client.reponse.goods.ChannelGoodsResultDTO | 见结构体 | 见结构体 |

### `data` (ChannelGoodsResultDTO)

| 名称 | 类型 | 描述 | 示例值 |
|:---|:---|:---|:---|
| `merchant_code` | String | 商家编码 | test1234 |
| `store_code` | String | 经营店编码 | store123 |
| `channelGoodsSyncReqList` | message:com.alibaba.tc.goods.cosmos.api.dto.open.ChannelGoodsSyncReq[] | 商品列表 | [{},{}] |
| `page` | Integer | 当前页 | 0 |
| `total` | Integer | 商品总数 | 20 |
| `page_size` | Integer | pageSize | 20 |

### `channelGoodsSyncReqList` 元素 (ChannelGoodsSyncReq)

| 名称 | 类型 | 描述 | 示例值 |
|:---|:---|:---|:---|
| `merchant_code` | String | 商家编码 | test1234 |
| `store_code` | String | 经营店编码 | store123 |
| `channel_type` | String | 渠道类型 | 如POS |
| `out_shop_id` | String | 外部商家标识 | abcd |
| `multi_spec` | Boolean | 是否多规格 | false |
| `status` | Integer | 商品上下架状态【已废弃】 | 4, "上架"；3, "下架" |
| `front_category_list` | message:me.ele.retail.saas.CategoryDTO[] | 前台类目列表 | [{"category_name":"水果","category_id":"111"},{"category_name":"肉类","category_id":"222"}] |
| `title` | String | 商品名称 | 可口可乐 |
| `main_pic` | String | 商品主图 | http://xxxx.png |
| `sub_pics` | String[] | 商品副图列表 | ["http://xxxx.png"] |
| `brand_name` | String | 品牌名称 | abc |
| `brand_id` | String | 品牌Id | 123 |
| `description` | String | 商品描述 | xxxxx |
| `alias_name` | String | 简称 | aaaa |
| `picture_content` | String | 商品图文详情 | xxxxx |
| `spu_code` | String | 商家标品编码 | abcd |
| `sku_list` | message:com.alibaba.tc.goods.cosmos.api.dto.open.ChannelSkuSyncReq[] | 商品详情 | [{}] |

### `front_category_list` 元素 (CategoryDTO)

| 名称 | 类型 | 描述 | 示例值 |
|:---|:---|:---|:---|
| `category_id` | Long | 类目id（发品时设置叶子类目） | 1234 |
| `category_name` | String | 类目名称 | 碳酸饮料 |
| `category_code` | String | 外部厂商对应的类目code | abcd |

### `sku_list` 元素 (ChannelSkuSyncReq)

| 名称 | 类型 | 描述 | 示例值 |
|:---|:---|:---|:---|
| `sub_sku_code` | String | 店内码 | abcd |
| `barcode_list` | String[] | 条码列表 | ["69123","68124"] |
| `sale_price` | Long | 销售价格，单位为分【已废弃】 | 100 |
| `weight` | String | 重量，单位为克/g | 250 |
| `specification` | String | 多规格商品规格名称 | 香辣味 |
| `inventory_unit` | String | 库存单位 | 个 |
| `sku_code` | String | 商品编码 | abcd |
| `channel_backend_category_id` | String | 后台类目id | 123456 |
| `goods_life_cycle` | message:goodsLifeCycleDTO | 效期 | {} |
| `period` | Integer | 效期天数 | 8 |
```