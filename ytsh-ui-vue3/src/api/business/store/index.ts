import request from '@/config/axios'
import type { Dayjs } from 'dayjs';

/** 门店简单信息 */
export interface StoreSimpleRespVO {
  storeId?: string; // 门店ID
  storeName: string; // 门店名称
  platformId?: number; // 平台ID
  platformStoreId?: string; // 平台方门店ID
  storeStatus?: number; // 门店状态(0停用1正常)
}

/** 门店空间信息 */
export interface SpaceTable {
          storeSpaceId: number; // 空间ID
          storeId: string; // 门店ID
          buildingArea: number; // 房屋面积(㎡)
          coldStorageArea: number; // 冷库面积(㎡)
}

/** 门店架构归属信息 */
export interface AffiliationTable {
          affiliationId: number; // 归属ID
          storeId: string; // 门店ID
          businessMode: string; // 经营方式(DIRECT/AGENCY/SELF/JOINT)
          storeType: string; // 门店类型(ONLINE/O2O)
}

/** 门店经营状态信息 */
export interface StatusTable {
          storeBusinessStatusId: number; // 状态ID
          storeId: string; // 门店ID
          currentStatus: string; // 当前状态(NORMAL/CLOSED)
          openDate: string | Dayjs; // 开业日期
          signDate: string | Dayjs; // 签约日期
}

/** 门店加盟商信息信息 */
export interface FranchiseeTable {
          franchiseeId: number; // 加盟ID
          storeId: string; // 门店ID
          franchiseeName: string; // 加盟商名称
          franchiseePhone: string; // 加盟联系方式
          franchiseeFee: number; // 加盟费
          securityDeposit: number; // 保证金
          contractStart: string | Dayjs; // 合同开始日期
          contractEnd: string | Dayjs; // 合同结束日期
}

/** 门店联系人通讯录信息 */
export interface ContactTable {
          contactId: number; // 联系人ID
          storeId: string; // 门店ID
          contactName: string; // 联系人姓名
          contactType: string; // 联系人类型(COMPANY/STORE/SUPPLIER/LOGISTICS/OTHER)
          contactRole: string; // 业务角色(OPERATION/SUPERVISOR/FINANCE/OWNER/MANAGER/PROCUREMENT/WAREHOUSE)
          phone: string; // 联系电话
          isPrimary: number; // 是否主要联系人(0否1是)
          status: number; // 状态(0禁用1启用)
          remark: string; // 备注
}

/** 门店平台关联信息 */
export interface PlatformTable {
          storePlatformId: number; // 平台关联ID
          storeId: string; // 门店ID
          platformId: number; // 平台ID
          platformName?: string; // 平台名称
          platformStoreId: string; // 平台门店ID
          platformStoreName: string; // 平台门店名称
          agentType: string; // 代理商类型
          commissionRate: number; // 佣金比例
          settlementAccount: string; // 结算账户
          status: number; // 状态
}

/** 门店信息 */
export interface Table {
          storeId: string; // 门店ID
          storeName: string; // 门店名称
          regionCode: string; // 行政区划代码
          regionName: string; // 行政区划名称
          address: string; // 详细地址
          area: string; // 门店区域(EAST华东/NORTH华北/SOUTH华南/WEST华西/CENTRAL华中)
          storeStatus: number; // 状态(0停用1正常)
          spaceTable?: SpaceTable | null
          affiliationTable?: AffiliationTable | null
          statusTable?: StatusTable | null
          franchiseeTable?: FranchiseeTable | null
          contactTables?: ContactTable[]
          spacetable?: SpaceTable
          affiliationtable?: AffiliationTable
          statustable?: StatusTable
          franchiseetable?: FranchiseeTable
          contacttables?: ContactTable[]
}

// 门店 API
export const TableApi = {
  // 查询门店分页
  getTablePage: async (params: any) => {
    return await request.get({ url: `/business/table/page`, params })
  },

  // 查询门店简单列表（用于搜索建议）
  getTableSimpleList: async (keyword?: string) => {
    return await request.get({ url: `/business/table/list-simple`, params: { keyword } })
  },

  // 获取所有门店简单信息列表
  getTableAllSimpleList: async (platformId?: number) => {
    return await request.get({ url: `/business/table/list-all-simple`, params: { platformId } })
  },

  // 查询门店详情
  getTable: async (id: number | string) => {
    return await request.get({ url: `/business/table/get?id=` + id })
  },

  // 新增门店
  createTable: async (data: Table) => {
    return await request.post({ url: `/business/table/create`, data })
  },

  // 修改门店
  updateTable: async (data: Table) => {
    return await request.put({ url: `/business/table/update`, data })
  },

  // 删除门店
  deleteTable: async (id: number | string) => {
    return await request.delete({ url: `/business/table/delete?id=` + id })
  },

  /** 批量删除门店 */
  deleteTableList: async (ids: (number | string)[]) => {
    return await request.delete({ url: `/business/table/delete-list?ids=${ids.join(',')}` })
  },

  // 导出门店 Excel
  exportTable: async (params) => {
    return await request.download({ url: `/business/table/export-excel`, params })
  },

  // 获得导入门店模板
  getImportTemplate: async (format?: string) => {
    const params = format ? { format } : {}
    return await request.download({ url: `/business/table/get-import-template`, params })
  },

  // 导入门店
  importTable: async (file: File, updateSupport?: boolean) => {
    const formData = new FormData()
    formData.append('file', file)
    if (updateSupport !== undefined) {
      formData.append('updateSupport', String(updateSupport))
    }
    return await request.post({ url: `/business/table/import`, data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
  },

// ==================== 子表（门店空间） ====================

  // 获得门店空间
  getSpaceTableByStoreId: async (storeId) => {
    return await request.get({ url: `/business/table/space-table/get-by-store-id?storeId=` + storeId })
  },

// ==================== 子表（门店架构归属） ====================

  // 获得门店架构归属
  getAffiliationTableByStoreId: async (storeId) => {
    return await request.get({ url: `/business/table/affiliation-table/get-by-store-id?storeId=` + storeId })
  },

// ==================== 子表（门店经营状态） ====================

  // 获得门店经营状态
  getStatusTableByStoreId: async (storeId) => {
    return await request.get({ url: `/business/table/status-table/get-by-store-id?storeId=` + storeId })
  },

// ==================== 子表（门店加盟商信息） ====================

  // 获得门店加盟商信息
  getFranchiseeTableByStoreId: async (storeId) => {
    return await request.get({ url: `/business/table/franchisee-table/get-by-store-id?storeId=` + storeId })
  },

// ==================== 子表（门店联系人通讯录） ====================

  // 获得门店联系人通讯录列表
  getContactTableListByStoreId: async (storeId) => {
    return await request.get({ url: `/business/table/contact-table/list-by-store-id?storeId=` + storeId })
  },

// ==================== 子表（门店平台关联） ====================

  // 获得门店平台关联列表
  getPlatformTableListByStoreId: async (storeId) => {
    return await request.get({ url: `/business/table/platform-table/list-by-store-id?storeId=` + storeId })
  }
}