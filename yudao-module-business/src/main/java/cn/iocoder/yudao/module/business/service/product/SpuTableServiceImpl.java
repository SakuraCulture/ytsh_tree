package cn.iocoder.yudao.module.business.service.product;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import cn.iocoder.yudao.module.business.controller.admin.product.vo.*;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SpuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.product.UpcTableDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.business.dal.mysql.product.SpuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.product.UpcTableMapper;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.*;

/**
 * SPU基础分类 Service 实现类
 *
 */
@Service
@Validated
public class SpuTableServiceImpl implements SpuTableService {

    @Resource
    private SpuTableMapper spuTableMapper;
    @Resource
    private SkuTableMapper skuTableMapper;
    @Resource
    private UpcTableMapper upcTableMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSpuTable(SpuTableSaveReqVO createReqVO) {
        // 插入
        SpuTableDO spuTable = BeanUtils.toBean(createReqVO, SpuTableDO.class);
        spuTableMapper.insert(spuTable);


        // 插入子表
        createSkuTableList(spuTable.getProductSpuId(), createReqVO.getSkuTables());
        // 返回
        return spuTable.getProductSpuId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSpuTable(SpuTableSaveReqVO updateReqVO) {
        // 校验存在
        validateSpuTableExists(updateReqVO.getProductSpuId());
        // 更新
        SpuTableDO updateObj = BeanUtils.toBean(updateReqVO, SpuTableDO.class);
        spuTableMapper.updateById(updateObj);

        // 更新子表
        updateSkuTableList(updateReqVO.getProductSpuId(), updateReqVO.getSkuTables());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSpuTable(Long id) {
        // 校验存在
        validateSpuTableExists(id);
        // 删除
        spuTableMapper.deleteById(id);

        // 删除子表
        deleteSkuTableByProductSpuId(id);
    }

    @Override
        @Transactional(rollbackFor = Exception.class)
    public void deleteSpuTableListByIds(List<Long> ids) {
        // 删除
        spuTableMapper.deleteByIds(ids);
    
    // 删除子表
            deleteSkuTableByProductSpuIds(ids);
    }


    private void validateSpuTableExists(Long id) {
        if (spuTableMapper.selectById(id) == null) {
            throw exception(SPU_TABLE_NOT_EXISTS);
        }
    }

    @Override
    public SpuTableDO getSpuTable(Long id) {
        return spuTableMapper.selectById(id);
    }

    @Override
    public PageResult<SpuTableDO> getSpuTablePage(SpuTablePageReqVO pageReqVO) {
        if (StrUtil.isNotEmpty(pageReqVO.getProductSkuCode())
                || StrUtil.isNotEmpty(pageReqVO.getProductSkuName())) {
            List<SkuTableDO> skuList = skuTableMapper.selectList(
                    new LambdaQueryWrapperX<SkuTableDO>()
                            .likeIfPresent(SkuTableDO::getProductSkuCode, pageReqVO.getProductSkuCode())
                            .likeIfPresent(SkuTableDO::getProductSkuName, pageReqVO.getProductSkuName())
            );
            if (CollUtil.isEmpty(skuList)) {
                return PageResult.empty();
            }
            List<Long> spuIds = convertList(skuList, SkuTableDO::getProductSpuId);
            pageReqVO.setSpuIds(CollUtil.distinct(spuIds));
        }
        return spuTableMapper.selectPage(pageReqVO);
    }

    // ==================== 子表（SKU商品主数据） ====================

    @Override
    public List<SkuTableDO> getSkuTableListByProductSpuId(Long productSpuId) {
        return skuTableMapper.selectListByProductSpuId(productSpuId);
    }

    private void createSkuTableList(Long productSpuId, List<SkuTableDO> list) {
        list.forEach(o -> o.setProductSpuId(productSpuId).clean());
        skuTableMapper.insertBatch(list);
    }

    private void updateSkuTableList(Long productSpuId, List<SkuTableDO> list) {
	    list.forEach(o -> o.setProductSpuId(productSpuId).clean());
	    List<SkuTableDO> oldList = skuTableMapper.selectListByProductSpuId(productSpuId);
	    List<List<SkuTableDO>> diffList = diffList(oldList, list, (oldVal, newVal) -> {
            boolean same = ObjectUtil.equal(oldVal.getProductSkuId(), newVal.getProductSkuId());
            if (same) {
                newVal.setProductSkuId(oldVal.getProductSkuId()).clean();
            }
            return same;
	    });

	    // 第二步，批量添加、修改、删除
	    if (CollUtil.isNotEmpty(diffList.get(0))) {
	        skuTableMapper.insertBatch(diffList.get(0));
	    }
	    if (CollUtil.isNotEmpty(diffList.get(1))) {
	        skuTableMapper.updateBatch(diffList.get(1));
	    }
	    if (CollUtil.isNotEmpty(diffList.get(2))) {
	        skuTableMapper.deleteByIds(convertList(diffList.get(2), SkuTableDO::getProductSkuId));
	    }
    }

    private void deleteSkuTableByProductSpuId(Long productSpuId) {
        skuTableMapper.deleteByProductSpuId(productSpuId);
    }

	private void deleteSkuTableByProductSpuIds(List<Long> productSpuIds) {
        skuTableMapper.deleteByProductSpuIds(productSpuIds);
	}

    @Override
    public SpuImportRespVO importSpuSkuUpcList(List<SpuSkuUpcImportVO> list, Boolean updateSupport) {
        if (list == null || list.isEmpty()) {
            return SpuImportRespVO.builder()
                .spuSuccessCount(0).skuSuccessCount(0).upcSuccessCount(0)
                .failureList(new ArrayList<>()).build();
        }

        int spuSuccessCount = 0;
        int skuSuccessCount = 0;
        int upcSuccessCount = 0;
        List<SpuImportRespVO.ImportFailure> failureList = new ArrayList<>();

        // 缓存已创建的SPU和SKU，避免重复创建
        Map<String, Long> spuCache = new HashMap<>();
        Map<String, Long> skuCache = new HashMap<>();

        for (int i = 0; i < list.size(); i++) {
            SpuSkuUpcImportVO vo = list.get(i);
            try {
                // 校验SPU必填字段
                if (vo.getProductSpuCode() == null || vo.getProductSpuCode().isEmpty()) {
                    throw new Exception("SPU编码不能为空");
                }

                // 处理SPU - 优先使用缓存
                Long spuId = spuCache.get(vo.getProductSpuCode());
                if (spuId == null) {
                    SpuTableDO existSpu = spuTableMapper.selectByProductSpuCode(vo.getProductSpuCode());
                    if (existSpu != null) {
                        if (updateSupport) {
                            BeanUtils.copyProperties(vo, existSpu);
                            existSpu.clean();
                            spuTableMapper.updateById(existSpu);
                            spuId = existSpu.getProductSpuId();
                            spuSuccessCount++;
                        } else {
                            spuId = existSpu.getProductSpuId();
                        }
                    } else {
                        SpuTableDO spu = SpuTableDO.builder()
                            .productSpuCode(vo.getProductSpuCode())
                            .productSpuName(vo.getProductSpuName())
                            .productBrand(vo.getProductBrand())
                            .categoryId(vo.getCategoryId())
                            .productOrigin(vo.getProductOrigin())
                            .productManufacturer(vo.getProductManufacturer())
                            .productSpecTemplate(vo.getProductSpecTemplate())
                            .productSpuStatus(vo.getProductSpuStatus())
                            .build();
                        spu.clean();
                        spuTableMapper.insert(spu);
                        spuId = spu.getProductSpuId();
                        spuSuccessCount++;
                    }
                    spuCache.put(vo.getProductSpuCode(), spuId);
                }

                // 处理SKU - 优先使用缓存（同一SPU下的SKU）
                Long skuId = null;
                if (vo.getProductSkuCode() != null && !vo.getProductSkuCode().isEmpty()) {
                    String skuCacheKey = spuId + "_" + vo.getProductSkuCode();
                    skuId = skuCache.get(skuCacheKey);
                    if (skuId == null) {
                        SkuTableDO existSku = skuTableMapper.selectByProductSkuCode(vo.getProductSkuCode());
                        if (existSku != null) {
                            if (updateSupport) {
                                existSku.setProductSkuName(vo.getProductSkuName());
                                existSku.setProductSkuEan(vo.getProductSkuEan());
                                existSku.setProductWeight(vo.getProductWeight());
                                existSku.setProductWeightUnit(vo.getProductWeightUnit());
                                existSku.setProductLength(vo.getProductLength());
                                existSku.setProductWidth(vo.getProductWidth());
                                existSku.setProductHeight(vo.getProductHeight());
                                existSku.setProductCostPrice(vo.getProductCostPrice());
                                existSku.setProductRetailPrice(vo.getProductRetailPrice());
                                existSku.setProductSkuStatus(vo.getProductSkuStatus());
                                existSku.clean();
                                skuTableMapper.updateById(existSku);
                                skuId = existSku.getProductSkuId();
                                skuSuccessCount++;
                            } else {
                                skuId = existSku.getProductSkuId();
                            }
                        } else {
                            SkuTableDO sku = SkuTableDO.builder()
                                .productSpuId(spuId)
                                .productSkuCode(vo.getProductSkuCode())
                                .productSkuName(vo.getProductSkuName())
                                .productSkuEan(vo.getProductSkuEan())
                                .productWeight(vo.getProductWeight())
                                .productWeightUnit(vo.getProductWeightUnit())
                                .productLength(vo.getProductLength())
                                .productWidth(vo.getProductWidth())
                                .productHeight(vo.getProductHeight())
                                .productCostPrice(vo.getProductCostPrice())
                                .productRetailPrice(vo.getProductRetailPrice())
                                .productSkuStatus(vo.getProductSkuStatus())
                                .build();
                            sku.clean();
                            skuTableMapper.insert(sku);
                            skuId = sku.getProductSkuId();
                            skuSuccessCount++;
                        }
                        skuCache.put(skuCacheKey, skuId);
                    }
                }

                // 处理UPC
                if (skuId != null && vo.getProductUpcValue() != null && !vo.getProductUpcValue().isEmpty()) {
                    // 查询是否已存在相同UPC码
                    List<UpcTableDO> existUpcs = upcTableMapper.selectList(
                        new LambdaQueryWrapperX<UpcTableDO>()
                            .eq(UpcTableDO::getProductSkuId, skuId)
                            .eq(UpcTableDO::getProductUpcValue, vo.getProductUpcValue())
                    );
                    if (existUpcs.isEmpty()) {
                        UpcTableDO upc = UpcTableDO.builder()
                            .productSkuId(skuId)
                            .productUpcType(vo.getProductUpcType())
                            .productUpcValue(vo.getProductUpcValue())
                            .productUpcIsPrimary(vo.getProductUpcIsPrimary())
                            .productUpcStatus(vo.getProductUpcStatus())
                            .build();
                        upc.clean();
                        upcTableMapper.insert(upc);
                        upcSuccessCount++;
                    }
                }

            } catch (Exception e) {
                failureList.add(SpuImportRespVO.ImportFailure.builder()
                    .row(i + 1).message(e.getMessage()).build());
            }
        }

        return SpuImportRespVO.builder()
            .spuSuccessCount(spuSuccessCount)
            .skuSuccessCount(skuSuccessCount)
            .upcSuccessCount(upcSuccessCount)
            .failureList(failureList)
            .build();
    }

    @Override
    public List<SpuSkuUpcExportVO> getExportData(SpuTablePageReqVO pageReqVO) {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<SpuTableDO> spuPage = getSpuTablePage(pageReqVO);
        if (spuPage.getList().isEmpty()) {
            return new ArrayList<>();
        }

        List<SpuTableDO> spuList = spuPage.getList();
        List<Long> spuIds = convertList(spuList, SpuTableDO::getProductSpuId);

        List<SkuTableDO> skuList = skuTableMapper.selectListByProductSpuIds(spuIds);
        Map<Long, List<SkuTableDO>> skuMap = skuList.stream().collect(Collectors.groupingBy(SkuTableDO::getProductSpuId));

        List<Long> skuIds = convertList(skuList, SkuTableDO::getProductSkuId);
        List<UpcTableDO> upcList = upcTableMapper.selectListByProductSkuIds(skuIds);
        Map<Long, List<UpcTableDO>> upcMap = upcList.stream().collect(Collectors.groupingBy(UpcTableDO::getProductSkuId));

        List<SpuSkuUpcExportVO> result = new ArrayList<>();
        for (SpuTableDO spu : spuList) {
            List<SkuTableDO> spuSkus = skuMap.getOrDefault(spu.getProductSpuId(), Collections.emptyList());
            if (spuSkus.isEmpty()) {
                result.add(buildExportVO(spu, null, null));
            } else {
                for (SkuTableDO sku : spuSkus) {
                    List<UpcTableDO> skuUpcs = upcMap.getOrDefault(sku.getProductSkuId(), Collections.emptyList());
                    if (skuUpcs.isEmpty()) {
                        result.add(buildExportVO(spu, sku, null));
                    } else {
                        for (UpcTableDO upc : skuUpcs) {
                            result.add(buildExportVO(spu, sku, upc));
                        }
                    }
                }
            }
        }
        return result;
    }

    private SpuSkuUpcExportVO buildExportVO(SpuTableDO spu, SkuTableDO sku, UpcTableDO upc) {
        SpuSkuUpcExportVO vo = SpuSkuUpcExportVO.builder()
            .productSpuId(spu.getProductSpuId())
            .productSpuCode(spu.getProductSpuCode())
            .productSpuName(spu.getProductSpuName())
            .productBrand(spu.getProductBrand())
            .categoryId(spu.getCategoryId())
            .productOrigin(spu.getProductOrigin())
            .productManufacturer(spu.getProductManufacturer())
            .productSpecTemplate(spu.getProductSpecTemplate())
            .productSpuStatus(spu.getProductSpuStatus())
            .createTime(spu.getCreateTime())
            .build();

        if (sku != null) {
            vo.setProductSkuCode(sku.getProductSkuCode());
            vo.setProductSkuName(sku.getProductSkuName());
            vo.setProductSkuEan(sku.getProductSkuEan());
            vo.setProductWeight(sku.getProductWeight());
            vo.setProductWeightUnit(sku.getProductWeightUnit());
            vo.setProductLength(sku.getProductLength());
            vo.setProductWidth(sku.getProductWidth());
            vo.setProductHeight(sku.getProductHeight());
            vo.setProductCostPrice(sku.getProductCostPrice());
            vo.setProductRetailPrice(sku.getProductRetailPrice());
            vo.setSkuImageUrl(sku.getProductImageUrl());
            vo.setProductSkuStatus(sku.getProductSkuStatus());
        }

        if (upc != null) {
            vo.setProductUpcType(upc.getProductUpcType());
            vo.setProductUpcValue(upc.getProductUpcValue());
            vo.setProductUpcIsPrimary(upc.getProductUpcIsPrimary());
            vo.setProductUpcStatus(upc.getProductUpcStatus());
        }

        return vo;
    }

}