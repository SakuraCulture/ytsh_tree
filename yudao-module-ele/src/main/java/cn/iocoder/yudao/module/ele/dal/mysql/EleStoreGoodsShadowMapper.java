package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsShadowPageReqVO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsShadowDO;
import cn.iocoder.yudao.module.ele.enums.EleStoreGoodsShadowStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface EleStoreGoodsShadowMapper extends BaseMapperX<EleStoreGoodsShadowDO> {

    default EleStoreGoodsShadowDO selectByBizKey(Long platformId, String merchantCode, String erpStoreCode, String skuCode) {
        return selectOne(new LambdaQueryWrapperX<EleStoreGoodsShadowDO>()
                .eq(EleStoreGoodsShadowDO::getPlatformId, platformId)
                .eq(EleStoreGoodsShadowDO::getMerchantCode, merchantCode)
                .eq(EleStoreGoodsShadowDO::getErpStoreCode, erpStoreCode)
                .eq(EleStoreGoodsShadowDO::getSkuCode, skuCode));
    }

    default EleStoreGoodsShadowDO selectByIdAndErpStoreCode(Long id, String erpStoreCode) {
        return selectOne(new LambdaQueryWrapperX<EleStoreGoodsShadowDO>()
                .eq(EleStoreGoodsShadowDO::getId, id)
                .eq(EleStoreGoodsShadowDO::getErpStoreCode, erpStoreCode));
    }

    default List<EleStoreGoodsShadowDO> selectActiveList(Collection<String> matchStatuses, String storeId,
                                                         String erpStoreCode, String skuCode, String title) {
        return selectList(new LambdaQueryWrapperX<EleStoreGoodsShadowDO>()
                .inIfPresent(EleStoreGoodsShadowDO::getMatchStatus, matchStatuses)
                .eqIfPresent(EleStoreGoodsShadowDO::getStoreId, storeId)
                .eqIfPresent(EleStoreGoodsShadowDO::getErpStoreCode, erpStoreCode)
                .likeIfPresent(EleStoreGoodsShadowDO::getSkuCode, skuCode)
                .likeIfPresent(EleStoreGoodsShadowDO::getTitle, title)
                .orderByDesc(EleStoreGoodsShadowDO::getUpdateTime));
    }

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM ele_store_goods_shadow s
            WHERE s.deleted = b'0'
              <if test="tenantId != null">
                AND s.tenant_id = #{tenantId}
              </if>
              AND s.match_status IN
              <foreach collection="matchStatuses" item="status" open="(" separator="," close=")">
                #{status}
              </foreach>
              <if test="storeId != null and storeId != ''">
                AND s.store_id = #{storeId}
              </if>
              <if test="skuCode != null and skuCode != ''">
                AND s.sku_code LIKE CONCAT('%', #{skuCode}, '%')
              </if>
              <if test="title != null and title != ''">
                AND s.title LIKE CONCAT('%', #{title}, '%')
              </if>
              <if test="matchStatus != null and matchStatus != ''">
                AND s.match_status = #{matchStatus}
              </if>
              <if test="excludeFormalRows">
                AND NOT EXISTS (
                  SELECT 1
                  FROM store_product_table sp
                  JOIN product_sku_table sku ON sku.product_sku_id = sp.product_sku_id
                  WHERE sp.deleted = 0
                    AND sku.deleted = 0
                    AND sp.store_id = s.store_id
                    AND sku.product_sku_code = s.sku_code
                )
              </if>
            </script>
            """)
    long selectActiveCount(@Param("matchStatuses") Collection<String> matchStatuses,
                           @Param("tenantId") Long tenantId,
                           @Param("storeId") String storeId,
                           @Param("skuCode") String skuCode,
                           @Param("title") String title,
                           @Param("matchStatus") String matchStatus,
                           @Param("excludeFormalRows") boolean excludeFormalRows);

    @Select("""
            <script>
            SELECT s.id,
                   s.store_id,
                   s.erp_store_code,
                   s.platform_store_id,
                   s.sku_code,
                   s.spu_code,
                   s.title,
                   s.specification,
                   s.sale_price,
                   s.pos_status,
                   s.is_active,
                   s.match_status,
                   s.create_time
            FROM ele_store_goods_shadow s
            WHERE s.deleted = b'0'
              <if test="tenantId != null">
                AND s.tenant_id = #{tenantId}
              </if>
              AND s.match_status IN
              <foreach collection="matchStatuses" item="status" open="(" separator="," close=")">
                #{status}
              </foreach>
              <if test="storeId != null and storeId != ''">
                AND s.store_id = #{storeId}
              </if>
              <if test="skuCode != null and skuCode != ''">
                AND s.sku_code LIKE CONCAT('%', #{skuCode}, '%')
              </if>
              <if test="title != null and title != ''">
                AND s.title LIKE CONCAT('%', #{title}, '%')
              </if>
              <if test="matchStatus != null and matchStatus != ''">
                AND s.match_status = #{matchStatus}
              </if>
              <if test="excludeFormalRows">
                AND NOT EXISTS (
                  SELECT 1
                  FROM store_product_table sp
                  JOIN product_sku_table sku ON sku.product_sku_id = sp.product_sku_id
                  WHERE sp.deleted = 0
                    AND sku.deleted = 0
                    AND sp.store_id = s.store_id
                    AND sku.product_sku_code = s.sku_code
                )
              </if>
            ORDER BY s.update_time DESC
            LIMIT #{offset}, #{limit}
            </script>
            """)
    List<EleStoreGoodsShadowDO> selectActivePage(@Param("matchStatuses") Collection<String> matchStatuses,
                                                @Param("tenantId") Long tenantId,
                                                @Param("storeId") String storeId,
                                                @Param("skuCode") String skuCode,
                                                @Param("title") String title,
                                                @Param("matchStatus") String matchStatus,
                                                @Param("excludeFormalRows") boolean excludeFormalRows,
                                                @Param("offset") int offset,
                                                @Param("limit") int limit);

    default PageResult<EleStoreGoodsShadowDO> selectPage(EleStoreGoodsShadowPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EleStoreGoodsShadowDO>()
                .likeIfPresent(EleStoreGoodsShadowDO::getMerchantCode, reqVO.getMerchantCode())
                .likeIfPresent(EleStoreGoodsShadowDO::getErpStoreCode, reqVO.getErpStoreCode())
                .likeIfPresent(EleStoreGoodsShadowDO::getPlatformStoreId, reqVO.getPlatformStoreId())
                .likeIfPresent(EleStoreGoodsShadowDO::getStoreId, reqVO.getStoreId())
                .likeIfPresent(EleStoreGoodsShadowDO::getSkuCode, reqVO.getSkuCode())
                .likeIfPresent(EleStoreGoodsShadowDO::getTitle, reqVO.getTitle())
                .eqIfPresent(EleStoreGoodsShadowDO::getMatchStatus, reqVO.getMatchStatus())
                .orderByDesc(EleStoreGoodsShadowDO::getUpdateTime));
    }

    default List<String> selectActiveSkuCodesByErpStoreCode(String erpStoreCode) {
        return selectList(new LambdaQueryWrapperX<EleStoreGoodsShadowDO>()
                .eqIfPresent(EleStoreGoodsShadowDO::getErpStoreCode, erpStoreCode)
                .eq(EleStoreGoodsShadowDO::getIsActive, 1)
                .ne(EleStoreGoodsShadowDO::getMatchStatus, EleStoreGoodsShadowStatus.UNMATCHED)
                .orderByDesc(EleStoreGoodsShadowDO::getUpdateTime))
                .stream()
                .map(EleStoreGoodsShadowDO::getSkuCode)
                .filter(StrUtil::isNotBlank)
                .toList();
    }
}
