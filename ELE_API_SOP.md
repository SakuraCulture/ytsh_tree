# ELE 开放接口对接 SOP（基于当前仓库实现）

> 目的：沉淀当前仓库里 `yudao-module-ele` 对接 ELE 第三方接口的真实实现方式，方便后续按同一套路新增其他 ELE API。
> 
> 本文档不是泛泛而谈，而是直接对应当前项目中已经跑通的订单列表 / 订单详情链路。

---

## 1. 先看当前项目里的真实调用链

### 1.1 远程订单列表调用链

1. 控制器入口：`yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleOrderController.java`
   - `getOrderListRemote(...)`
   - 路径：`GET /ele/order/list/remote`
2. Service 主入口：`yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleOrderServiceImpl.java`
   - `getOrderList(OrderListReqDTO req)`
3. 读取有效配置：
   - `getApiConfig()`
   - 从 `ele_api_config` 表中取 `status = 1` 的最新一条配置
4. 如果传了 `platformStoreId`：
   - 调 `storeService.getPlatformTableByPlatformStoreId(platformStoreId)`
   - 从门店平台映射中取：
     - `settlementAccount` → 用作 `merchant_code`
     - 当前订单查询实现里通常把 `platformStoreId` 作为 `erp_store_code`
5. 组装请求 body：
   - `MeEleRetailSaasOrderListReqDto`
6. 组装第三方请求对象：
   - `SaasOrderListParam extends AbstractAPIRequest<SaasOrderListResult>`
7. 创建执行器并发请求：
   - `new ApiExecutor<>(config.getAppId(), config.getAppSecret())`
   - `executor.send(param)`
8. 校验返回：
   - `wrapper != null`
   - `wrapper.getBody() != null`
   - `errno == 0`
9. 转内部 DTO：
   - `convertListResult(...)`
10. 继续补详情：
   - `enrichOrderListWithDetails(...)`
   - 对列表中的每个订单再次调详情接口，把摘要补全成完整订单

### 1.2 远程订单详情调用链

1. 控制器入口：`EleOrderController#getOrderDetail(...)`
   - 路径：`GET /ele/order/detail`
2. Service 入口：`EleOrderServiceImpl#getOrderDetail(...)`
3. 优先查本地：
   - `getDetailFromLocal(orderId)`
4. 本地没有则查远程：
   - `getOrderDetailRemote(platformStoreId, merchantCode, erpStoreCode, orderId)`
5. 组装请求 body：
   - `SaasOrderGetParam.SaasOrderGetBody`
6. 发请求：
   - `ApiExecutor<SaasOrderGetResult>`
7. 校验返回：
   - `errno == 0`
8. 转内部 DTO：
   - `convertDetailResult(...)`
9. 回写本地数据库：
   - `saveRemoteOrderDetailToLocal(...)`
   - 同步更新主订单、平台信息、子订单、优惠信息、状态日志

---

## 2. 当前项目真正依赖了哪些三方包

位置：`yudao-module-ele/pom.xml`

### 2.1 核心 SDK / Jar

1. `com.alibaba.ocean:ocean.client.java.biz`
   - 本地 jar：`yudao-module-ele/src/main/resources/lib/ocean.client.java.biz.jar`
   - 这是当前项目直连 ELE 开放接口的核心 SDK

2. `com.alibaba:fastjson:1.2.48`
   - 本地 jar：`yudao-module-ele/src/main/resources/lib/fastjson-1.2.48.jar`
   - Ocean SDK 依赖

3. `commons-logging:commons-logging:1.1.1`
   - 本地 jar：`yudao-module-ele/src/main/resources/lib/commons-logging-1.1.1.jar`
   - Ocean SDK 依赖

### 2.2 代码里直接用到的 SDK 类

- `com.alibaba.ocean.rawsdk.ApiExecutor`
- `com.alibaba.ocean.rawsdk.common.BizResultWrapper`
- `com.alibaba.ocean.rawsdk.common.AbstractAPIRequest`
- `com.alibaba.ocean.rawsdk.client.APIId`

### 2.3 当前项目内部封装包

位置：`yudao-module-ele/src/main/java/lib/ele/retail/param/`

这里放的是“每个 API 对应的请求/响应模型”，当前已存在：

- `SaasOrderListParam`
- `SaasOrderListResult`
- `MeEleRetailSaasOrderListReqDto`
- `MeEleRetailSaasOrderListResDto`
- `MeEleRetailSaasOrderListDetailResDto`
- `SaasOrderGetParam`
- `SaasOrderGetResult`

**以后新增其他 ELE API，也应该继续按这个目录和模式补。**

---

## 3. 对接前必须准备的配置

### 3.1 数据库配置表：`ele_api_config`

实体：`yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/dataobject/EleApiConfig.java`

当前订单链路里真正会用到的核心字段：

- `appId`
- `appSecret`
- `merchantCode`（兜底）
- `status`（必须有一条 `status = 1` 的有效配置）

读取逻辑：
- `EleApiConfigMapper#selectActive()`

### 3.2 门店映射配置

如果你的接口是按门店维度调用，当前项目默认会走：

- `storeService.getPlatformTableByPlatformStoreId(platformStoreId)`

返回对象：
- `StorePlatformRespVO`

里面重点字段：
- `platformStoreId`
- `storeId`
- `settlementAccount`

### 3.3 一个非常重要的注意点

当前项目里 **merchantCode / erpStoreCode / platformStoreId 的语义并不是完全等价的**，新增接口时一定要先确认三方文档要求的是哪个字段。

当前订单查询链路的真实实现是：

- `merchant_code` ← `settlementAccount`
- `erp_store_code` ← 当前订单列表/详情逻辑里通常直接传 `platformStoreId`

但在其他同步逻辑里，也出现过 `storeId` 的使用。

**结论：对接新 API 时，不要机械照抄 store 字段，先看三方文档要求，再决定传 `platformStoreId` 还是 `storeId`。**

---

## 4. 一个 ELE API 请求通常要哪些“必填字段”

这里分成三层来看。

### 4.1 第 1 层：执行器认证参数（必需）

来自 `EleApiConfig`：

- `appId`
- `appSecret`

当前代码：

```java
ApiExecutor<XXXResult> executor = new ApiExecutor<>(config.getAppId(), config.getAppSecret());
```

### 4.2 第 2 层：请求外层公共字段（当前实现固定带）

当前项目里，每次请求都会给 `AbstractAPIRequest` 派生对象设置：

- `ticket`
- `encrypt = "aes"`
- `body`

当前代码模式：

```java
param.setTicket(UUID.randomUUID().toString().toUpperCase());
param.setEncrypt("aes");
param.setBody(body);
```

### 4.3 第 3 层：业务 body 字段（按 API 不同而不同）

#### 订单列表 API：`saas.order.list`

body 类型：`MeEleRetailSaasOrderListReqDto`

当前仓库里最重要字段：

- `merchant_code`：ERP 接入场景必填
- `erp_store_code`：ERP 接入场景必填
- `start_time`：秒级时间戳
- `end_time`：秒级时间戳
- `page_size`：分页条数，SDK 注释写明范围 `1-100`
- `scroll_id`：翻页游标，第一页可空
- `status`：可选
- `market_user_id`：服务市场场景字段，当前项目订单链路没有使用

#### 订单详情 API：`saas.order.get`

body 类型：`SaasOrderGetParam.SaasOrderGetBody`

当前仓库里最重要字段：

- `order_id`：必填
- `merchant_code`：必填
- `erp_store_code`：必填
- `market_user_id`：当前项目未使用

---

## 5. 时间戳单位规则（非常关键）

### 5.1 调第三方 ELE 远程 API 时

当前 SDK 注释和后端订单远程实现都表明：

- `start_time`
- `end_time`

使用的是 **Unix 秒级时间戳**。

例如：

```java
body.setStart_time(startTime);
body.setEnd_time(endTime);
```

如果没有传时间，`EleOrderServiceImpl#getOrderList(...)` 的默认兜底也是按 **秒级** 算的：

```java
startTime = calendar.getTimeInMillis() / 1000;
endTime = System.currentTimeMillis() / 1000;
```

### 5.2 当前前端本地订单页的本地库查询

前端本地订单页查 `/ele/order/list` 时，你前面已经确认过，本项目本地查询链路实际用的是 **毫秒级**。

### 5.3 结论

- **调第三方 ELE API：优先按秒级理解**
- **调本项目本地 DB 查询接口：看本项目接口实现，当前订单页实际是毫秒级**

新增 ELE API 时，**不要把本地 DB 接口的时间单位习惯带到第三方 SDK 请求里**。

---

## 6. 新增一个 ELE API 的标准技术 SOP

下面是推荐的标准步骤。

### 第 1 步：先确认三方接口文档

你必须先确认：

1. API 名称（如 `saas.xxx.yyy`）
2. namespace（当前订单接口是 `me.ele.retail`）
3. version（当前订单接口是 `3`）
4. body 字段列表
5. 哪些字段是 ERP 场景必填
6. 时间字段是秒级还是毫秒级
7. 是否支持游标分页
8. 返回体的 `errno / error / data` 结构

### 第 2 步：在 `lib/ele/retail/param/` 下建请求类

参照：
- `SaasOrderListParam.java`
- `SaasOrderGetParam.java`

标准写法：

```java
public class XxxApiParam extends AbstractAPIRequest<XxxApiResult> {

    private XxxApiBody body;

    public XxxApiParam() {
        super();
        oceanApiId = new APIId("me.ele.retail", "saas.xxx.yyy", 3);
    }

    public XxxApiBody getBody() {
        return body;
    }

    public void setBody(XxxApiBody body) {
        this.body = body;
    }
}
```

### 第 3 步：补 body DTO / result DTO

请求 DTO 参照：
- `MeEleRetailSaasOrderListReqDto.java`

返回 DTO 参照：
- `SaasOrderListResult.java`
- `SaasOrderGetResult.java`

建议规则：

- 字段名尽量直接贴近第三方原始字段，如 `merchant_code`
- 不要过早改成驼峰，先保证和三方协议一一对应
- 返回体保留：
  - `errno`
  - `error`
  - `data`

### 第 4 步：在 Service 里新增调用方法

最推荐的仿照对象：
- `EleOrderServiceImpl#getOrderList(...)`
- `EleOrderServiceImpl#getOrderDetailRemote(...)`

标准模板如下：

```java
public XxxRespDTO callXxx(String platformStoreId, String merchantCode, String erpStoreCode, ...) {
    EleApiConfig config = getApiConfig();

    // 1. 归一化门店参数
    String finalMerchantCode = merchantCode;
    String finalErpStoreCode = erpStoreCode;
    if (platformStoreId != null && !platformStoreId.isEmpty()) {
        StorePlatformRespVO platformInfo = storeService.getPlatformTableByPlatformStoreId(platformStoreId);
        if (platformInfo == null) {
            throw new RuntimeException("未找到平台门店ID对应的门店信息: " + platformStoreId);
        }
        finalMerchantCode = platformInfo.getSettlementAccount();
        // 这里要根据三方文档确认到底传 platformStoreId 还是 storeId
        finalErpStoreCode = platformStoreId;
    }

    // 2. 配置兜底
    if (finalMerchantCode == null || finalMerchantCode.isEmpty()) {
        finalMerchantCode = config.getMerchantCode();
    }

    // 3. 必填校验
    if (finalMerchantCode == null || finalMerchantCode.isEmpty()) {
        throw new RuntimeException("merchantCode不能为空");
    }
    if (finalErpStoreCode == null || finalErpStoreCode.isEmpty()) {
        throw new RuntimeException("erpStoreCode不能为空");
    }

    // 4. 组 body
    XxxBody body = new XxxBody();
    body.setMerchant_code(finalMerchantCode);
    body.setErp_store_code(finalErpStoreCode);
    // body.setXxx(...)

    // 5. 组 param
    XxxApiParam param = new XxxApiParam();
    param.setTicket(UUID.randomUUID().toString().toUpperCase());
    param.setEncrypt("aes");
    param.setBody(body);

    try {
        ApiExecutor<XxxApiResult> executor = new ApiExecutor<>(config.getAppId(), config.getAppSecret());
        BizResultWrapper<XxxApiResult> wrapper = executor.send(param);

        if (wrapper == null || wrapper.getBody() == null) {
            throw new RuntimeException("翱象接口返回数据为空");
        }

        XxxApiResult result = wrapper.getBody();
        if (result.getErrno() != null && !"0".equals(result.getErrno())) {
            throw new RuntimeException("翱象接口返回错误[" + result.getErrno() + "]: " + result.getError());
        }

        // 6. 转内部 DTO
        return convertXxxResult(result);
    } catch (Exception e) {
        saveFailRecord(null, null, "XXX", "API", e.getMessage(), param, null, 0, null);
        throw new RuntimeException("调用翱象接口失败: " + e.getMessage(), e);
    }
}
```

### 第 5 步：按需补转换层

如果新接口返回结构比较大，建议单独做一层转换：

- 远程原始返回 DTO
- 内部统一 DTO / VO
- 数据库存储 DO

可以参考：
- `EleOrderConvertService`
- `convertListResult(...)`
- `convertDetailResult(...)`

### 第 6 步：按需补控制器

如果要给前端/管理后台暴露接口，就在：

- `EleOrderController`

或者新建对应 Controller。

建议保持这种结构：

- `/xxx/list/remote`：第三方实时查
- `/xxx/list`：本地库查
- `/xxx/detail`：本地优先、远程兜底

### 第 7 步：按需补失败记录和重试能力

当前项目已经有成熟范式：

- `saveFailRecord(...)`
- 失败记录表：`EleOrderFailRecord`
- 失败分页 / 重试接口：已经在订单链路里实现

如果新 API 很关键，建议照搬这个模式，不要只打日志。

### 第 8 步：如果是列表型接口，优先考虑分页拉全

当前订单列表控制器 `getOrderListRemote(...)` 的做法是：

1. 第一次请求不传 `scrollId`
2. 拿到返回里的 `scrollId`
3. 继续循环请求下一页
4. 直到：
   - `scrollId` 为空
   - `scrollId` 重复
   - 当前页为空

所以如果你以后对接的也是“游标分页”接口，直接照这个套路做就可以。

---

## 7. 当前项目里最值得直接仿照的文件

### 7.1 必看文件

1. 控制器
   - `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleOrderController.java`

2. 主 service
   - `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleOrderServiceImpl.java`

3. 请求 DTO
   - `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/dto/OrderListReqDTO.java`

4. 远程参数模型
   - `yudao-module-ele/src/main/java/lib/ele/retail/param/SaasOrderListParam.java`
   - `yudao-module-ele/src/main/java/lib/ele/retail/param/SaasOrderGetParam.java`
   - `yudao-module-ele/src/main/java/lib/ele/retail/param/MeEleRetailSaasOrderListReqDto.java`

5. 远程结果模型
   - `yudao-module-ele/src/main/java/lib/ele/retail/param/SaasOrderListResult.java`
   - `yudao-module-ele/src/main/java/lib/ele/retail/param/SaasOrderGetResult.java`

6. 配置读取
   - `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/dataobject/EleApiConfig.java`
   - `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/mysql/EleApiConfigMapper.java`

7. 门店映射
   - `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StorePlatformRespVO.java`

8. 本地转换层
   - `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleOrderConvertService.java`

### 7.2 直接照抄的优先级建议

如果你要新接一个 ELE API，优先照抄下面这条线：

1. `SaasOrderGetParam` / `SaasOrderListParam` 的写法
2. `EleOrderServiceImpl#getOrderList(...)` 的调用骨架
3. `convertListResult(...)` / `convertDetailResult(...)` 的结果校验方式
4. `saveFailRecord(...)` 的异常记录方式
5. `EleOrderController` 的接口暴露风格

---

## 8. 当前项目里“没有手工做”的事，也要知道

当前订单链路里，没有看到手工拼下面这些字段：

- `sign`
- `timestamp`
- `token`
- `accessToken`
- `developerId`
- `apiUrl`

虽然这些字段在 `EleApiConfig` 或 `EleApiProperties` 中出现了，但**当前订单查询这条链路没有直接使用它们**。

也就是说，当前项目的真实做法是：

- 主要依赖 `ApiExecutor(appId, appSecret)`
- 以及 `AbstractAPIRequest` 上的 `ticket + encrypt + body`

所以：

**新增接口时优先复用当前 SDK 调用模式，不要一上来就自己手写签名。**

除非三方新接口明确要求，而且现有 SDK 模式跑不通，再考虑补自定义签名逻辑。

---

## 9. 常见坑位总结

### 坑 1：把本地接口时间单位和第三方接口时间单位混了

- 本地库查询：当前项目很多场景是毫秒级
- 第三方 ELE API：当前订单接口按秒级

### 坑 2：把 `platformStoreId`、`storeId`、`erpStoreCode` 混为一谈

新增接口前先确认：

- 三方接口到底要哪个门店字段
- 当前项目映射里哪个字段才对应它

### 坑 3：只查列表，不补详情

订单列表远程接口返回的是摘要信息；当前项目会继续逐条查详情补全。新 API 如果列表也只是摘要，同样要补详情。

### 坑 4：不做 `errno` 校验

一定要校验：

- `wrapper != null`
- `wrapper.getBody() != null`
- `errno == 0`

### 坑 5：不做失败记录

当前项目不是只打日志，而是把失败请求和错误信息落表。关键接口建议延续这个模式，便于排障和重试。

### 坑 6：分页型接口只查第一页

只查第一页通常会误以为“接口没问题但数据不全”。如果三方返回 `scroll_id`，就按游标拉全。

---

## 10. 最终推荐的新增 ELE API 实施清单

你以后要对接任何其他 ELE API，可以按这个 checklist 走：

- [ ] 先确认三方文档里的 `namespace / apiName / version`
- [ ] 确认 body 的必填字段
- [ ] 确认时间字段是秒级还是毫秒级
- [ ] 确认门店字段到底是 `platformStoreId / storeId / erpStoreCode / market_user_id` 哪个
- [ ] 在 `lib/ele/retail/param/` 下补 `Param / Result / Body DTO`
- [ ] 在 service 中照 `getOrderList(...)` 写调用骨架
- [ ] 用 `ApiExecutor<>(appId, appSecret)` 发请求
- [ ] 校验 `errno`
- [ ] 转内部 DTO/VO
- [ ] 关键接口补失败记录
- [ ] 列表型接口处理分页/游标
- [ ] 如需长期使用，补本地落库和查询接口

---

## 11. 一句话总结

**当前项目对接 ELE API 的标准范式就是：**

> `读 ele_api_config → 归一化门店参数 → 组 body DTO → 组 AbstractAPIRequest(APIId) → setTicket/setEncrypt → ApiExecutor(appId, appSecret).send() → 校验 errno → 转内部 DTO → 视情况落库 / 失败记录 / 分页补拉 / 详情补全`。

后续你接其他 ELE API，核心上只需要替换三样东西：

1. `APIId`
2. `body DTO`
3. `result DTO`

其余框架都可以沿用当前订单链路。

---

## 12. 最小可复制模板版（可以直接照着新建）

这一节给你一套**最小可复制骨架**。以后你新增任意一个 ELE API，优先按这个模板建文件。

### 12.1 推荐新增文件结构

假设你要接一个新接口，名字先用占位符 `saas.xxx.yyy`。

建议新增这些文件：

```text
yudao-module-ele/
├─ src/main/java/lib/ele/retail/param/
│  ├─ SaasXxxYyyParam.java
│  ├─ SaasXxxYyyResult.java
│  └─ SaasXxxYyyReqDto.java
├─ src/main/java/cn/iocoder/yudao/module/ele/service/dto/
│  └─ XxxYyyRespDTO.java
├─ src/main/java/cn/iocoder/yudao/module/ele/service/
│  └─ EleXxxService.java / 或直接扩到现有 Service
└─ src/main/java/cn/iocoder/yudao/module/ele/controller/admin/
   └─ EleXxxController.java / 或扩到现有 Controller
```

如果这个新接口返回结构很简单，也可以先不单独建 `XxxYyyRespDTO`，直接在 Service 中转成现有 VO/DTO。

### 12.2 最小 Param 模板

```java
package lib.ele.retail.param;

import com.alibaba.ocean.rawsdk.client.APIId;
import com.alibaba.ocean.rawsdk.common.AbstractAPIRequest;

public class SaasXxxYyyParam extends AbstractAPIRequest<SaasXxxYyyResult> {

    private SaasXxxYyyReqDto body;

    public SaasXxxYyyParam() {
        super();
        oceanApiId = new APIId("me.ele.retail", "saas.xxx.yyy", 3);
    }

    public SaasXxxYyyReqDto getBody() {
        return body;
    }

    public void setBody(SaasXxxYyyReqDto body) {
        this.body = body;
    }
}
```

### 12.3 最小请求 DTO 模板

```java
package lib.ele.retail.param;

public class SaasXxxYyyReqDto {

    private String merchant_code;
    private String erp_store_code;
    private String biz_id;
    private Long start_time;
    private Long end_time;

    public String getMerchant_code() {
        return merchant_code;
    }

    public void setMerchant_code(String merchant_code) {
        this.merchant_code = merchant_code;
    }

    public String getErp_store_code() {
        return erp_store_code;
    }

    public void setErp_store_code(String erp_store_code) {
        this.erp_store_code = erp_store_code;
    }

    public String getBiz_id() {
        return biz_id;
    }

    public void setBiz_id(String biz_id) {
        this.biz_id = biz_id;
    }

    public Long getStart_time() {
        return start_time;
    }

    public void setStart_time(Long start_time) {
        this.start_time = start_time;
    }

    public Long getEnd_time() {
        return end_time;
    }

    public void setEnd_time(Long end_time) {
        this.end_time = end_time;
    }
}
```

### 12.4 最小返回 DTO 模板

```java
package lib.ele.retail.param;

public class SaasXxxYyyResult {

    private String errno;
    private String error;
    private SaasXxxYyyData data;

    public String getErrno() {
        return errno;
    }

    public void setErrno(String errno) {
        this.errno = errno;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public SaasXxxYyyData getData() {
        return data;
    }

    public void setData(SaasXxxYyyData data) {
        this.data = data;
    }

    public static class SaasXxxYyyData {
        private String biz_id;
        private String status;
        private String message;

        public String getBiz_id() {
            return biz_id;
        }

        public void setBiz_id(String biz_id) {
            this.biz_id = biz_id;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
```

### 12.5 最小内部响应 DTO 模板

```java
package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;

@Data
public class XxxYyyRespDTO {
    private String bizId;
    private String status;
    private String message;
}
```

### 12.6 最小 Service 调用模板

```java
public XxxYyyRespDTO callXxxYyy(String platformStoreId, String merchantCode, String erpStoreCode, String bizId) {
    EleApiConfig config = getApiConfig();

    String finalMerchantCode = merchantCode;
    String finalErpStoreCode = erpStoreCode;

    if (platformStoreId != null && !platformStoreId.isEmpty()) {
        StorePlatformRespVO platformInfo = storeService.getPlatformTableByPlatformStoreId(platformStoreId);
        if (platformInfo == null) {
            throw new RuntimeException("未找到平台门店ID对应的门店信息: " + platformStoreId);
        }
        finalMerchantCode = platformInfo.getSettlementAccount();
        finalErpStoreCode = platformStoreId;
    }

    if (finalMerchantCode == null || finalMerchantCode.isEmpty()) {
        finalMerchantCode = config.getMerchantCode();
    }
    if (finalMerchantCode == null || finalMerchantCode.isEmpty()) {
        throw new RuntimeException("merchantCode不能为空");
    }
    if (finalErpStoreCode == null || finalErpStoreCode.isEmpty()) {
        throw new RuntimeException("erpStoreCode不能为空");
    }
    if (bizId == null || bizId.isEmpty()) {
        throw new RuntimeException("bizId不能为空");
    }

    SaasXxxYyyReqDto body = new SaasXxxYyyReqDto();
    body.setMerchant_code(finalMerchantCode);
    body.setErp_store_code(finalErpStoreCode);
    body.setBiz_id(bizId);

    SaasXxxYyyParam param = new SaasXxxYyyParam();
    param.setTicket(java.util.UUID.randomUUID().toString().toUpperCase());
    param.setEncrypt("aes");
    param.setBody(body);

    try {
        ApiExecutor<SaasXxxYyyResult> executor = new ApiExecutor<>(config.getAppId(), config.getAppSecret());
        BizResultWrapper<SaasXxxYyyResult> wrapper = executor.send(param);

        if (wrapper == null || wrapper.getBody() == null) {
            throw new RuntimeException("翱象接口返回数据为空");
        }

        SaasXxxYyyResult result = wrapper.getBody();
        if (result.getErrno() != null && !"0".equals(result.getErrno())) {
            throw new RuntimeException("翱象接口返回错误[" + result.getErrno() + "]: " + result.getError());
        }

        SaasXxxYyyResult.SaasXxxYyyData data = result.getData();
        if (data == null) {
            throw new RuntimeException("翱象接口 data 为空");
        }

        XxxYyyRespDTO dto = new XxxYyyRespDTO();
        dto.setBizId(data.getBiz_id());
        dto.setStatus(data.getStatus());
        dto.setMessage(data.getMessage());
        return dto;
    } catch (Exception e) {
        saveFailRecord(null, null, "XXX_YYY", "API", e.getMessage(), param, null, 0, null);
        throw new RuntimeException("调用 saas.xxx.yyy 失败: " + e.getMessage(), e);
    }
}
```

### 12.7 最小 Controller 模板

```java
@GetMapping("/xxx/yyy")
@Operation(summary = "调用 ELE saas.xxx.yyy")
public CommonResult<XxxYyyRespDTO> callXxxYyy(
        @RequestParam(required = false) String platformStoreId,
        @RequestParam(required = false) String merchantCode,
        @RequestParam(required = false) String erpStoreCode,
        @RequestParam String bizId) {
    return CommonResult.success(eleOrderService.callXxxYyy(platformStoreId, merchantCode, erpStoreCode, bizId));
}
```

### 12.8 最小可复制模板的使用方法

以后你只要做下面这些替换：

- `SaasXxxYyyParam` → 改成真实类名
- `saas.xxx.yyy` → 改成真实 API 名
- `biz_id` / `status` / `message` → 改成真实业务字段
- `XxxYyyRespDTO` → 改成真实内部 DTO
- 如果有分页，就补 `page_size / scroll_id`
- 如果是详情接口，就补唯一主键如 `order_id / shop_id / product_id`

---

## 13. 实战样板代码版（占位 API，可直接替换成真实接口）

> 注意：这一节用的是**占位 API 样板**，不是在声明真实存在某个 `saas.shop.xxx` 接口。
> 
> 目的是给你一个“拿来就改”的完整范例。真正落地时，把 API 名、字段名替换成你要接的 ELE 文档字段即可。

下面以“新增一个**按门店查询某业务对象详情**”的接口为例，演示完整实施流程。

### 13.1 场景假设

假设三方文档定义了一个接口：

- namespace：`me.ele.retail`
- apiName：`saas.demo.detail`
- version：`3`

请求 body 假设是：

- `merchant_code`（必填）
- `erp_store_code`（必填）
- `demo_id`（必填）

返回 data 假设是：

- `demo_id`
- `demo_name`
- `status`
- `update_time`

### 13.2 第一步：新增 Param 文件

文件：`lib/ele/retail/param/SaasDemoDetailParam.java`

```java
package lib.ele.retail.param;

import com.alibaba.ocean.rawsdk.client.APIId;
import com.alibaba.ocean.rawsdk.common.AbstractAPIRequest;

public class SaasDemoDetailParam extends AbstractAPIRequest<SaasDemoDetailResult> {

    private SaasDemoDetailReqDto body;

    public SaasDemoDetailParam() {
        super();
        oceanApiId = new APIId("me.ele.retail", "saas.demo.detail", 3);
    }

    public SaasDemoDetailReqDto getBody() {
        return body;
    }

    public void setBody(SaasDemoDetailReqDto body) {
        this.body = body;
    }
}
```

### 13.3 第二步：新增请求 DTO

文件：`lib/ele/retail/param/SaasDemoDetailReqDto.java`

```java
package lib.ele.retail.param;

public class SaasDemoDetailReqDto {

    private String merchant_code;
    private String erp_store_code;
    private String demo_id;

    public String getMerchant_code() {
        return merchant_code;
    }

    public void setMerchant_code(String merchant_code) {
        this.merchant_code = merchant_code;
    }

    public String getErp_store_code() {
        return erp_store_code;
    }

    public void setErp_store_code(String erp_store_code) {
        this.erp_store_code = erp_store_code;
    }

    public String getDemo_id() {
        return demo_id;
    }

    public void setDemo_id(String demo_id) {
        this.demo_id = demo_id;
    }
}
```

### 13.4 第三步：新增返回 DTO

文件：`lib/ele/retail/param/SaasDemoDetailResult.java`

```java
package lib.ele.retail.param;

public class SaasDemoDetailResult {

    private String errno;
    private String error;
    private Data data;

    public String getErrno() {
        return errno;
    }

    public void setErrno(String errno) {
        this.errno = errno;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {
        private String demo_id;
        private String demo_name;
        private String status;
        private Long update_time;

        public String getDemo_id() {
            return demo_id;
        }

        public void setDemo_id(String demo_id) {
            this.demo_id = demo_id;
        }

        public String getDemo_name() {
            return demo_name;
        }

        public void setDemo_name(String demo_name) {
            this.demo_name = demo_name;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getUpdate_time() {
            return update_time;
        }

        public void setUpdate_time(Long update_time) {
            this.update_time = update_time;
        }
    }
}
```

### 13.5 第四步：新增内部 DTO

文件：`cn/iocoder/yudao/module/ele/service/dto/DemoDetailRespDTO.java`

```java
package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;

@Data
public class DemoDetailRespDTO {
    private String demoId;
    private String demoName;
    private String status;
    private Long updateTime;
}
```

### 13.6 第五步：在 Service 中新增调用方法

```java
public DemoDetailRespDTO getDemoDetail(String platformStoreId, String merchantCode, String erpStoreCode, String demoId) {
    EleApiConfig config = getApiConfig();

    String finalMerchantCode = merchantCode;
    String finalErpStoreCode = erpStoreCode;

    if (platformStoreId != null && !platformStoreId.isEmpty()) {
        StorePlatformRespVO platformInfo = storeService.getPlatformTableByPlatformStoreId(platformStoreId);
        if (platformInfo == null) {
            throw new RuntimeException("未找到平台门店ID对应的门店信息: " + platformStoreId);
        }
        finalMerchantCode = platformInfo.getSettlementAccount();
        finalErpStoreCode = platformStoreId;
    }

    if (finalMerchantCode == null || finalMerchantCode.isEmpty()) {
        finalMerchantCode = config.getMerchantCode();
    }
    if (finalMerchantCode == null || finalMerchantCode.isEmpty()) {
        throw new RuntimeException("merchantCode不能为空");
    }
    if (finalErpStoreCode == null || finalErpStoreCode.isEmpty()) {
        throw new RuntimeException("erpStoreCode不能为空");
    }
    if (demoId == null || demoId.isEmpty()) {
        throw new RuntimeException("demoId不能为空");
    }

    SaasDemoDetailReqDto body = new SaasDemoDetailReqDto();
    body.setMerchant_code(finalMerchantCode);
    body.setErp_store_code(finalErpStoreCode);
    body.setDemo_id(demoId);

    SaasDemoDetailParam param = new SaasDemoDetailParam();
    param.setTicket(java.util.UUID.randomUUID().toString().toUpperCase());
    param.setEncrypt("aes");
    param.setBody(body);

    try {
        ApiExecutor<SaasDemoDetailResult> executor = new ApiExecutor<>(config.getAppId(), config.getAppSecret());
        BizResultWrapper<SaasDemoDetailResult> wrapper = executor.send(param);

        if (wrapper == null || wrapper.getBody() == null) {
            throw new RuntimeException("翱象接口返回数据为空");
        }

        SaasDemoDetailResult result = wrapper.getBody();
        if (result.getErrno() != null && !"0".equals(result.getErrno())) {
            throw new RuntimeException("翱象接口返回错误[" + result.getErrno() + "]: " + result.getError());
        }
        if (result.getData() == null) {
            throw new RuntimeException("翱象接口 data 为空");
        }

        DemoDetailRespDTO dto = new DemoDetailRespDTO();
        dto.setDemoId(result.getData().getDemo_id());
        dto.setDemoName(result.getData().getDemo_name());
        dto.setStatus(result.getData().getStatus());
        dto.setUpdateTime(result.getData().getUpdate_time());
        return dto;
    } catch (Exception e) {
        saveFailRecord(null, null, "DEMO_DETAIL", "API", e.getMessage(), param, null, 0, null);
        throw new RuntimeException("调用 ELE demo 详情接口失败: " + e.getMessage(), e);
    }
}
```

### 13.7 第六步：暴露控制器接口

```java
@GetMapping("/demo/detail")
@Operation(summary = "查询 ELE demo 详情")
public CommonResult<DemoDetailRespDTO> getDemoDetail(
        @RequestParam(required = false) String platformStoreId,
        @RequestParam(required = false) String merchantCode,
        @RequestParam(required = false) String erpStoreCode,
        @RequestParam String demoId) {
    return CommonResult.success(eleOrderService.getDemoDetail(platformStoreId, merchantCode, erpStoreCode, demoId));
}
```

### 13.8 如果这个新接口要落库，应该怎么接

如果你新增的 ELE 接口也需要落本地库，建议照订单链路拆成三层：

1. **远程调用层**
   - 负责发请求、校验 `errno`
2. **转换层**
   - 负责把三方原始字段转成内部 DTO / DO
3. **持久化层**
   - 负责 `insert/update` 和状态日志 / 失败记录

参考现有订单代码：

- `convertDetailResult(...)`
- `saveRemoteOrderDetailToLocal(...)`
- `EleOrderConvertService`

不要把“发请求 + 字段转换 + 落库 + 失败重试”全写到一个大方法里，否则后面维护很痛苦。

### 13.9 如果这个新接口是列表接口，应该额外补什么

如果你接的是列表接口，不要只写一次 `executor.send(param)`。

你至少要确认：

- 是否有 `scroll_id`
- 是否有页码/页大小
- 是否需要详情补查
- 是否需要限流

当前订单代码里已经有两个可以直接借鉴的点：

1. `scrollId` 循环拉全
2. `RateLimiter` 控制调用频率

如果你未来对接的是高频接口，建议也加类似：

```java
private static final RateLimiter XXX_RATE_LIMITER = RateLimiter.create(100);
```

然后在请求前：

```java
XXX_RATE_LIMITER.acquire();
```

### 13.10 实战样板落地时的替换清单

当你准备把这套 demo 样板换成真实接口时，按这个顺序替换最稳：

1. 先改 `APIId("me.ele.retail", "saas.xxx.yyy", 3)`
2. 再改请求 body 字段
3. 再改返回 data 字段
4. 再改内部 DTO 字段
5. 最后再决定要不要落库 / 分页 / 重试 / 详情补查

---

## 14. 你以后最省事的工作方法

如果你下次要接一个新的 ELE API，建议你直接按下面顺序操作：

1. 先把三方文档里的请求/返回字段贴出来
2. 对照本文第 12 节，先建最小模板文件
3. 跑通一次最小请求
4. 再补 controller
5. 再补本地落库
6. 最后再补重试、分页、日志

**先跑通最小闭环，再做增强，不要一上来就把所有能力一次性写满。**

这套节奏最适合你现在这种“要快速仿照订单接口去接别的 ELE 接口”的场景。
