package lib.ele.retail.param;

import java.util.*;

public class SaasOrderGetResult {

    private String errno;
    private String error;
    private SaasOrderGetData data;

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

    public SaasOrderGetData getData() {
        return data;
    }

    public void setData(SaasOrderGetData data) {
        this.data = data;
    }

    public static class SaasOrderGetData {
        private String order_id;
        private Long create_time;
        private Long pay_time;
        private String channel_source_name;
        private String buyer_name;
        private String buyer_phone;
        private String buyer_address;
        private String delivery_name;
        private String delivery_phone;
        private String delivery_platform;
        private Integer delivery_type;
        private Integer delivery_status;
        private Integer status;
        private Integer total_fee;
        private Integer pay_fee;
        private Integer discount_fee;
        private Integer delivery_fee;
        private Integer post_fee;
        private Integer package_fee;
        private Integer platform_commission_fee;
        private String scroll_id;
        private Long total;
        private SubOrder[] sub_orders;
        private Discount[] discounts;
        private String remark;
        private String channel_source_id;
        private String channel_order_id;
        private String channel_type;
        private String store_code;
        private String erp_store_code;
        private String longitude;
        private String latitude;
        private String user_id;
        private Integer arrive_type;
        private Integer estimated_income;

        public String getOrder_id() {
            return order_id;
        }

        public void setOrder_id(String order_id) {
            this.order_id = order_id;
        }

        public Long getCreate_time() {
            return create_time;
        }

        public void setCreate_time(Long create_time) {
            this.create_time = create_time;
        }

        public Long getPay_time() {
            return pay_time;
        }

        public void setPay_time(Long pay_time) {
            this.pay_time = pay_time;
        }

        public String getChannel_source_name() {
            return channel_source_name;
        }

        public void setChannel_source_name(String channel_source_name) {
            this.channel_source_name = channel_source_name;
        }

        public String getBuyer_name() {
            return buyer_name;
        }

        public void setBuyer_name(String buyer_name) {
            this.buyer_name = buyer_name;
        }

        public String getBuyer_phone() {
            return buyer_phone;
        }

        public void setBuyer_phone(String buyer_phone) {
            this.buyer_phone = buyer_phone;
        }

        public String getBuyer_address() {
            return buyer_address;
        }

        public void setBuyer_address(String buyer_address) {
            this.buyer_address = buyer_address;
        }

        public String getDelivery_name() {
            return delivery_name;
        }

        public void setDelivery_name(String delivery_name) {
            this.delivery_name = delivery_name;
        }

        public String getDelivery_phone() {
            return delivery_phone;
        }

        public void setDelivery_phone(String delivery_phone) {
            this.delivery_phone = delivery_phone;
        }

        public String getDelivery_platform() {
            return delivery_platform;
        }

        public void setDelivery_platform(String delivery_platform) {
            this.delivery_platform = delivery_platform;
        }

        public Integer getDelivery_type() {
            return delivery_type;
        }

        public void setDelivery_type(Integer delivery_type) {
            this.delivery_type = delivery_type;
        }

        public Integer getDelivery_status() {
            return delivery_status;
        }

        public void setDelivery_status(Integer delivery_status) {
            this.delivery_status = delivery_status;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public Integer getTotal_fee() {
            return total_fee;
        }

        public void setTotal_fee(Integer total_fee) {
            this.total_fee = total_fee;
        }

        public Integer getPay_fee() {
            return pay_fee;
        }

        public void setPay_fee(Integer pay_fee) {
            this.pay_fee = pay_fee;
        }

        public Integer getDiscount_fee() {
            return discount_fee;
        }

        public void setDiscount_fee(Integer discount_fee) {
            this.discount_fee = discount_fee;
        }

        public Integer getDelivery_fee() {
            return delivery_fee;
        }

        public void setDelivery_fee(Integer delivery_fee) {
            this.delivery_fee = delivery_fee;
        }

        public Integer getPost_fee() {
            return post_fee;
        }

        public void setPost_fee(Integer post_fee) {
            this.post_fee = post_fee;
        }

        public Integer getPackage_fee() {
            return package_fee;
        }

        public void setPackage_fee(Integer package_fee) {
            this.package_fee = package_fee;
        }

        public Integer getPlatform_commission_fee() {
            return platform_commission_fee;
        }

        public void setPlatform_commission_fee(Integer platform_commission_fee) {
            this.platform_commission_fee = platform_commission_fee;
        }

        public String getScroll_id() {
            return scroll_id;
        }

        public void setScroll_id(String scroll_id) {
            this.scroll_id = scroll_id;
        }

        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }

        public SubOrder[] getSub_orders() {
            return sub_orders;
        }

        public void setSub_orders(SubOrder[] sub_orders) {
            this.sub_orders = sub_orders;
        }

        public Discount[] getDiscounts() {
            return discounts;
        }

        public void setDiscounts(Discount[] discounts) {
            this.discounts = discounts;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public String getChannel_source_id() {
            return channel_source_id;
        }

        public void setChannel_source_id(String channel_source_id) {
            this.channel_source_id = channel_source_id;
        }

        public String getChannel_order_id() {
            return channel_order_id;
        }

        public void setChannel_order_id(String channel_order_id) {
            this.channel_order_id = channel_order_id;
        }

        public String getChannel_type() {
            return channel_type;
        }

        public void setChannel_type(String channel_type) {
            this.channel_type = channel_type;
        }

        public String getStore_code() {
            return store_code;
        }

        public void setStore_code(String store_code) {
            this.store_code = store_code;
        }

        public String getErp_store_code() {
            return erp_store_code;
        }

        public void setErp_store_code(String erp_store_code) {
            this.erp_store_code = erp_store_code;
        }

        public String getLongitude() {
            return longitude;
        }

        public void setLongitude(String longitude) {
            this.longitude = longitude;
        }

        public String getLatitude() {
            return latitude;
        }

        public void setLatitude(String latitude) {
            this.latitude = latitude;
        }

        public String getUser_id() {
            return user_id;
        }

        public void setUser_id(String user_id) {
            this.user_id = user_id;
        }

        public Integer getArrive_type() {
            return arrive_type;
        }

        public void setArrive_type(Integer arrive_type) {
            this.arrive_type = arrive_type;
        }

        public Integer getEstimated_income() {
            return estimated_income;
        }

        public void setEstimated_income(Integer estimated_income) {
            this.estimated_income = estimated_income;
        }
    }

    public static class SubOrder {
        private String sub_order_id;
        private String sku_code;
        private String sku_name;
        private String barcode;
        private String specification;
        private Integer price;
        private Integer total_fee;
        private Integer pay_fee;
        private Integer buy_amount;
        private String goods_type;
        private String cabinet_code;
        private Integer weight;
        private Integer num;

        public String getSub_order_id() {
            return sub_order_id;
        }

        public void setSub_order_id(String sub_order_id) {
            this.sub_order_id = sub_order_id;
        }

        public String getSku_code() {
            return sku_code;
        }

        public void setSku_code(String sku_code) {
            this.sku_code = sku_code;
        }

        public String getSku_name() {
            return sku_name;
        }

        public void setSku_name(String sku_name) {
            this.sku_name = sku_name;
        }

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public String getSpecification() {
            return specification;
        }

        public void setSpecification(String specification) {
            this.specification = specification;
        }

        public Integer getPrice() {
            return price;
        }

        public void setPrice(Integer price) {
            this.price = price;
        }

        public Integer getTotal_fee() {
            return total_fee;
        }

        public void setTotal_fee(Integer total_fee) {
            this.total_fee = total_fee;
        }

        public Integer getPay_fee() {
            return pay_fee;
        }

        public void setPay_fee(Integer pay_fee) {
            this.pay_fee = pay_fee;
        }

        public Integer getBuy_amount() {
            return buy_amount;
        }

        public void setBuy_amount(Integer buy_amount) {
            this.buy_amount = buy_amount;
        }

        public String getGoods_type() {
            return goods_type;
        }

        public void setGoods_type(String goods_type) {
            this.goods_type = goods_type;
        }

        public String getCabinet_code() {
            return cabinet_code;
        }

        public void setCabinet_code(String cabinet_code) {
            this.cabinet_code = cabinet_code;
        }

        public Integer getWeight() {
            return weight;
        }

        public void setWeight(Integer weight) {
            this.weight = weight;
        }

        public Integer getNum() {
            return num;
        }

        public void setNum(Integer num) {
            this.num = num;
        }
    }

    public static class Discount {
        private String activity_name;
        private Integer activity_order_type;
        private String activity_id;
        private String type;
        private Integer discount_fee;
        private Integer merchant_fee;
        private Integer platform_fee;

        public String getActivity_name() {
            return activity_name;
        }

        public void setActivity_name(String activity_name) {
            this.activity_name = activity_name;
        }

        public Integer getActivity_order_type() {
            return activity_order_type;
        }

        public void setActivity_order_type(Integer activity_order_type) {
            this.activity_order_type = activity_order_type;
        }

        public String getActivity_id() {
            return activity_id;
        }

        public void setActivity_id(String activity_id) {
            this.activity_id = activity_id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getDiscount_fee() {
            return discount_fee;
        }

        public void setDiscount_fee(Integer discount_fee) {
            this.discount_fee = discount_fee;
        }

        public Integer getMerchant_fee() {
            return merchant_fee;
        }

        public void setMerchant_fee(Integer merchant_fee) {
            this.merchant_fee = merchant_fee;
        }

        public Integer getPlatform_fee() {
            return platform_fee;
        }

        public void setPlatform_fee(Integer platform_fee) {
            this.platform_fee = platform_fee;
        }
    }
}
