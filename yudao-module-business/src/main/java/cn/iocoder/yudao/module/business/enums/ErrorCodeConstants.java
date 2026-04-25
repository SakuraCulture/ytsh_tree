package cn.iocoder.yudao.module.business.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * 业务模块错误码枚举类
 * <p>
 * business 系统，使用 1-021-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== SPU基础分类 1-021-000-000 ==========
    ErrorCode SPU_TABLE_NOT_EXISTS = new ErrorCode(1_021_000_000, "SPU基础分类不存在");

    // ========== SKU条码管理 1-021-001-000 ==========
    ErrorCode UPC_TABLE_NOT_EXISTS = new ErrorCode(1_021_001_000, "SKU条码不存在");
    ErrorCode UPC_VALUE_EXISTS = new ErrorCode(1_021_001_001, "条码值已存在");

    // ========== 门店管理 1-022-000-000 ==========
    ErrorCode STORE_NOT_EXISTS = new ErrorCode(1_022_000_000, "门店不存在");
    ErrorCode STORE_IMPORT_LIST_IS_EMPTY = new ErrorCode(1_022_001_000, "导入门店列表不能为空");
    ErrorCode STORE_CODE_EXISTS = new ErrorCode(1_022_001_001, "门店编码已存在");
    ErrorCode STORE_NAME_EXISTS = new ErrorCode(1_022_001_002, "门店名称已存在");

    // ========== 门店商品管理 1-024-000-000 ==========
    ErrorCode STORE_PRODUCT_NOT_EXISTS = new ErrorCode(1_024_000_000, "门店商品不存在");
    ErrorCode STORE_PRODUCT_IMPORT_LIST_IS_EMPTY = new ErrorCode(1_024_001_000, "导入门店商品列表不能为空");
    ErrorCode STORE_PRODUCT_SKU_EXISTS_IN_STORE = new ErrorCode(1_024_001_001, "该门店下已存在此SKU的入店记录");
    ErrorCode STORE_PRODUCT_DELETE_HAS_STOCK = new ErrorCode(1_024_001_002, "该门店商品存在库存，无法删除");

    // ========== 仓库管理 1-025-000-000 ==========
    ErrorCode WAREHOUSE_NOT_EXISTS = new ErrorCode(1_025_000_000, "仓库不存在");
    ErrorCode WAREHOUSE_CODE_EXISTS = new ErrorCode(1_025_001_000, "仓库编码已存在");
    ErrorCode WAREHOUSE_NAME_EXISTS = new ErrorCode(1_025_001_001, "仓库名称已存在");

    // ========== 仓库供应商管理 1-026-000-000 ==========
    ErrorCode WAREHOUSE_SUPPLIER_NOT_EXISTS = new ErrorCode(1_026_000_000, "供应商不存在");
    ErrorCode WAREHOUSE_SUPPLIER_NAME_EXISTS = new ErrorCode(1_026_001_000, "供应商名称已存在");

    // ========== 仓库商品管理 1-027-000-000 ==========
    ErrorCode WAREHOUSE_PRODUCT_NOT_EXISTS = new ErrorCode(1_027_000_000, "仓库商品不存在");
    ErrorCode WAREHOUSE_PRODUCT_SKU_EXISTS = new ErrorCode(1_027_001_000, "该仓库下已存在此 SKU");
    ErrorCode WAREHOUSE_PRODUCT_DELETE_HAS_STOCK = new ErrorCode(1_027_001_001, "该仓库商品存在库存，无法删除");
    ErrorCode WAREHOUSE_PRODUCT_SKU_NOT_EXISTS = new ErrorCode(1_027_001_002, "SKU 不存在");

    // ========== 仓库库存管理 1-028-000-000 ==========
    ErrorCode WAREHOUSE_STOCK_NOT_EXISTS = new ErrorCode(1_028_000_000, "仓库库存不存在");

    // ========== 仓库采购管理 1-029-000-000 ==========
    ErrorCode WAREHOUSE_PURCHASE_NOT_EXISTS = new ErrorCode(1_029_000_000, "采购单不存在");
    ErrorCode WAREHOUSE_PURCHASE_DETAIL_EMPTY = new ErrorCode(1_029_001_000, "采购单明细不能为空");
    ErrorCode WAREHOUSE_PURCHASE_UPDATE_FAIL = new ErrorCode(1_029_001_001, "当前状态不允许修改采购单");
    ErrorCode WAREHOUSE_PURCHASE_DELETE_FAIL = new ErrorCode(1_029_001_002, "当前状态不允许删除采购单");
    ErrorCode WAREHOUSE_PURCHASE_SUBMIT_FAIL = new ErrorCode(1_029_001_003, "当前状态不允许提交采购单");
    ErrorCode WAREHOUSE_PURCHASE_AUDIT_FAIL = new ErrorCode(1_029_001_004, "当前状态不允许审核采购单");
    ErrorCode WAREHOUSE_PURCHASE_INBOUND_FAIL = new ErrorCode(1_029_001_005, "当前状态不允许确认入库");
    ErrorCode WAREHOUSE_PURCHASE_CANCEL_FAIL = new ErrorCode(1_029_001_006, "当前状态不允许取消采购单");
    ErrorCode WAREHOUSE_PURCHASE_ORDER_NO_EXISTS = new ErrorCode(1_029_001_007, "采购单号已存在");
    ErrorCode WAREHOUSE_PURCHASE_SUPPLIER_NOT_EXISTS = new ErrorCode(1_029_001_008, "采购供应商不存在");

    // ========== 类目管理 1-023-000-000 ==========
    ErrorCode CATEGORY_TABLE_NOT_EXISTS = new ErrorCode(1-023-000-000, "商品类目表（三级树形结构）不存在");
    ErrorCode CATEGORY_TABLE_EXITS_CHILDREN = new ErrorCode(1-023-001-000, "存在存在子商品类目表（三级树形结构），无法删除");
    ErrorCode CATEGORY_TABLE_PARENT_NOT_EXITS = new ErrorCode(1-023-002-000,"父级商品类目表（三级树形结构）不存在");
    ErrorCode CATEGORY_TABLE_PARENT_ERROR = new ErrorCode(1-023-003-000, "不能设置自己为父商品类目表（三级树形结构）");
    ErrorCode CATEGORY_TABLE_CATEGORY_NAME_DUPLICATE = new ErrorCode(1-023-004-000, "已经存在该类目名称的商品类目表（三级树形结构）");
    ErrorCode CATEGORY_TABLE_PARENT_IS_CHILD = new ErrorCode(1-023-005-000, "不能设置自己的子CategoryTable为父CategoryTable");
    ErrorCode CATEGORY_TABLE_IMPORT_LIST_IS_EMPTY = new ErrorCode(1-023-006-000, "导入类目列表不能为空");
}