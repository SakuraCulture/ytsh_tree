package cn.iocoder.yudao.module.business.service.warehouse;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLineImportExcelVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLineImportRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLinePageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLineRespVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLineSaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseLineDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseLineStoreDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseStoreSupplyDO;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseLineMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseLineStoreMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseStoreSupplyMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.STORE_NOT_EXISTS;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_LINE_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_LINE_NOT_EXISTS;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_LINE_STORE_NOT_ELIGIBLE;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_LINE_WEEKDAY_CONFLICT;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_NOT_EXISTS;

@Service
@Validated
public class WarehouseLineServiceImpl implements WarehouseLineService {

    @Resource
    private WarehouseLineMapper warehouseLineMapper;
    @Resource
    private WarehouseLineStoreMapper warehouseLineStoreMapper;
    @Resource
    private WarehouseStoreSupplyMapper warehouseStoreSupplyMapper;
    @Resource
    private WarehouseMapper warehouseMapper;
    @Resource
    private StoreMapper storeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWarehouseLine(WarehouseLineSaveReqVO createReqVO) {
        validateWarehouseExists(createReqVO.getWarehouseId());
        validateLineCodeDuplicate(null, createReqVO.getWarehouseId(), createReqVO.getLineCode());
        List<String> normalizedStoreIds = normalizeStoreIds(createReqVO.getStoreIds());
        String normalizedWeekdays = normalizeWeekdays(createReqVO.getOrderWeekdays());
        validateStoresEligible(createReqVO.getWarehouseId(), normalizedStoreIds);
        validateSameWarehouseWeekdayConflict(null, createReqVO.getWarehouseId(), createReqVO.getLineStatus(), normalizedStoreIds, parseWeekdays(normalizedWeekdays));

        WarehouseLineDO line = BeanUtils.toBean(createReqVO, WarehouseLineDO.class, bean -> bean.setOrderWeekdays(normalizedWeekdays));
        warehouseLineMapper.insert(line);
        replaceLineStores(line.getLineId(), normalizedStoreIds);
        return line.getLineId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouseLine(WarehouseLineSaveReqVO updateReqVO) {
        WarehouseLineDO line = validateExists(updateReqVO.getLineId());
        validateWarehouseExists(updateReqVO.getWarehouseId());
        validateLineCodeDuplicate(updateReqVO.getLineId(), updateReqVO.getWarehouseId(), updateReqVO.getLineCode());
        List<String> normalizedStoreIds = normalizeStoreIds(updateReqVO.getStoreIds());
        String normalizedWeekdays = normalizeWeekdays(updateReqVO.getOrderWeekdays());
        validateStoresEligible(updateReqVO.getWarehouseId(), normalizedStoreIds);
        validateSameWarehouseWeekdayConflict(updateReqVO.getLineId(), updateReqVO.getWarehouseId(), updateReqVO.getLineStatus(), normalizedStoreIds, parseWeekdays(normalizedWeekdays));

        WarehouseLineDO updateObj = BeanUtils.toBean(updateReqVO, WarehouseLineDO.class, bean -> bean.setOrderWeekdays(normalizedWeekdays));
        warehouseLineMapper.updateById(updateObj);
        replaceLineStores(line.getLineId(), normalizedStoreIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWarehouseLine(Long lineId) {
        validateExists(lineId);
        warehouseLineStoreMapper.deleteByLineId(lineId);
        warehouseLineMapper.deleteById(lineId);
    }

    @Override
    public WarehouseLineRespVO getWarehouseLine(Long lineId) {
        return buildRespVO(validateExists(lineId));
    }

    @Override
    public PageResult<WarehouseLineRespVO> getWarehouseLinePage(WarehouseLinePageReqVO pageReqVO) {
        PageResult<WarehouseLineDO> pageResult = warehouseLineMapper.selectPageRaw(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }
        return new PageResult<>(pageResult.getList().stream().map(this::buildRespVO).collect(Collectors.toList()), pageResult.getTotal());
    }

    @Override
    public WarehouseLineImportRespVO importWarehouseLineList(List<WarehouseLineImportExcelVO> importList, boolean updateSupport) {
        WarehouseLineImportRespVO respVO = WarehouseLineImportRespVO.builder()
                .createCount(0)
                .updateCount(0)
                .failureCount(0)
                .failureRows(new LinkedHashMap<>())
                .build();
        if (CollUtil.isEmpty(importList)) {
            return respVO;
        }

        for (int i = 0; i < importList.size(); i++) {
            WarehouseLineImportExcelVO row = importList.get(i);
            String rowKey = "第 " + (i + 1) + " 行";
            try {
                if (row.getWarehouseId() == null || row.getWarehouseId().isBlank() || row.getLineCode() == null || row.getLineCode().isBlank()) {
                    throw new IllegalArgumentException("仓库ID和线路编码不能为空");
                }
                validateWarehouseExists(row.getWarehouseId());
                WarehouseLineDO line = warehouseLineMapper.selectByWarehouseIdAndLineCode(row.getWarehouseId(), row.getLineCode());
                if (line == null) {
                    createLineFromImport(row);
                    respVO.setCreateCount(respVO.getCreateCount() + 1);
                    continue;
                }

                List<WarehouseLineStoreDO> existingRelations = warehouseLineStoreMapper.selectListByLineId(line.getLineId());
                Set<String> finalStoreIds = existingRelations.stream()
                        .map(WarehouseLineStoreDO::getStoreId)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                if (hasStoreId(row)) {
                    validateStoresEligible(row.getWarehouseId(), Collections.singletonList(row.getStoreId()));
                    finalStoreIds.add(row.getStoreId());
                }

                String finalLineName = line.getLineName();
                String finalWeekdays = line.getOrderWeekdays();
                Integer finalStatus = line.getLineStatus();
                String finalRemark = line.getRemark();
                if (updateSupport) {
                    if (row.getLineName() != null && !row.getLineName().isBlank()) {
                        finalLineName = row.getLineName();
                    }
                    if (row.getOrderWeekdays() != null && !row.getOrderWeekdays().isBlank()) {
                        finalWeekdays = normalizeWeekdays(parseWeekdays(row.getOrderWeekdays()));
                    }
                    if (row.getLineStatus() != null) {
                        finalStatus = row.getLineStatus();
                    }
                    if (row.getRemark() != null) {
                        finalRemark = row.getRemark();
                    }
                }

                validateSameWarehouseWeekdayConflict(line.getLineId(), row.getWarehouseId(), finalStatus,
                        new ArrayList<>(finalStoreIds), parseWeekdays(finalWeekdays));

                if (updateSupport) {
                    boolean masterChanged = !Objects.equals(line.getLineName(), finalLineName)
                            || !Objects.equals(line.getOrderWeekdays(), finalWeekdays)
                            || !Objects.equals(line.getLineStatus(), finalStatus)
                            || !Objects.equals(line.getRemark(), finalRemark);
                    if (masterChanged) {
                        line.setLineName(finalLineName);
                        line.setOrderWeekdays(finalWeekdays);
                        line.setLineStatus(finalStatus);
                        line.setRemark(finalRemark);
                        warehouseLineMapper.updateById(line);
                        respVO.setUpdateCount(respVO.getUpdateCount() + 1);
                    }
                }
                bindStoreIfAbsent(line.getLineId(), row.getStoreId(), row.getSortNo(), existingRelations);
            } catch (Exception ex) {
                respVO.getFailureRows().put(rowKey, ex.getMessage());
                respVO.setFailureCount(respVO.getFailureCount() + 1);
            }
        }
        return respVO;
    }

    private WarehouseLineDO validateExists(Long lineId) {
        WarehouseLineDO line = warehouseLineMapper.selectById(lineId);
        if (line == null) {
            throw exception(WAREHOUSE_LINE_NOT_EXISTS);
        }
        return line;
    }

    private void validateWarehouseExists(String warehouseId) {
        if (warehouseMapper.selectById(warehouseId) == null) {
            throw exception(WAREHOUSE_NOT_EXISTS);
        }
    }

    private void validateLineCodeDuplicate(Long lineId, String warehouseId, String lineCode) {
        WarehouseLineDO line = warehouseLineMapper.selectByWarehouseIdAndLineCode(warehouseId, lineCode);
        if (line != null && !line.getLineId().equals(lineId)) {
            throw exception(WAREHOUSE_LINE_CODE_DUPLICATE);
        }
    }

    private List<String> normalizeStoreIds(List<String> storeIds) {
        return new ArrayList<>(new LinkedHashSet<>(storeIds));
    }

    private String normalizeWeekdays(Collection<Integer> weekdays) {
        return weekdays.stream().distinct().sorted().map(String::valueOf).collect(Collectors.joining(","));
    }

    private Set<Integer> parseWeekdays(String weekdays) {
        if (weekdays == null || weekdays.isBlank()) {
            return Collections.emptySet();
        }
        return java.util.Arrays.stream(weekdays.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(Integer::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean hasWeekdayOverlap(Set<Integer> left, Set<Integer> right) {
        return left.stream().anyMatch(right::contains);
    }

    private void validateStoresEligible(String warehouseId, List<String> storeIds) {
        if (CollUtil.isEmpty(storeIds)) {
            return;
        }
        Map<String, WarehouseStoreSupplyDO> relationMap = warehouseStoreSupplyMapper.selectListByWarehouseId(warehouseId).stream()
                .filter(item -> Integer.valueOf(1).equals(item.getSupplyStatus()))
                .collect(Collectors.toMap(WarehouseStoreSupplyDO::getStoreId, Function.identity(), (left, right) -> left));
        for (String storeId : storeIds) {
            if (storeMapper.selectById(storeId) == null) {
                throw exception(STORE_NOT_EXISTS);
            }
            if (!relationMap.containsKey(storeId)) {
                throw exception(WAREHOUSE_LINE_STORE_NOT_ELIGIBLE);
            }
        }
    }

    private void validateSameWarehouseWeekdayConflict(Long currentLineId, String warehouseId, Integer lineStatus, List<String> storeIds, Set<Integer> weekdays) {
        if (!Integer.valueOf(1).equals(lineStatus) || CollUtil.isEmpty(storeIds) || CollUtil.isEmpty(weekdays)) {
            return;
        }
        List<WarehouseLineDO> activeLines = warehouseLineMapper.selectActiveListByWarehouseId(warehouseId).stream()
                .filter(item -> !item.getLineId().equals(currentLineId))
                .filter(item -> hasWeekdayOverlap(weekdays, parseWeekdays(item.getOrderWeekdays())))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(activeLines)) {
            return;
        }
        Map<Long, List<WarehouseLineStoreDO>> lineStoreMap = warehouseLineStoreMapper.selectListByLineIds(
                        activeLines.stream().map(WarehouseLineDO::getLineId).collect(Collectors.toList()))
                .stream()
                .collect(Collectors.groupingBy(WarehouseLineStoreDO::getLineId));
        Set<String> targetStoreIds = new LinkedHashSet<>(storeIds);
        for (WarehouseLineDO activeLine : activeLines) {
            List<WarehouseLineStoreDO> bindings = lineStoreMap.getOrDefault(activeLine.getLineId(), Collections.emptyList());
            boolean conflicted = bindings.stream().map(WarehouseLineStoreDO::getStoreId).anyMatch(targetStoreIds::contains);
            if (conflicted) {
                throw exception(WAREHOUSE_LINE_WEEKDAY_CONFLICT);
            }
        }
    }

    private void replaceLineStores(Long lineId, List<String> storeIds) {
        warehouseLineStoreMapper.deleteByLineId(lineId);
        int sortNo = 1;
        for (String storeId : storeIds) {
            WarehouseLineStoreDO relation = new WarehouseLineStoreDO();
            relation.setLineId(lineId);
            relation.setStoreId(storeId);
            relation.setSortNo(sortNo++);
            warehouseLineStoreMapper.insert(relation);
        }
    }

    private WarehouseLineRespVO buildRespVO(WarehouseLineDO line) {
        WarehouseLineRespVO respVO = BeanUtils.toBean(line, WarehouseLineRespVO.class);
        respVO.setOrderWeekdays(new ArrayList<>(parseWeekdays(line.getOrderWeekdays())));

        WarehouseDO warehouse = warehouseMapper.selectById(line.getWarehouseId());
        if (warehouse != null) {
            respVO.setWarehouseName(warehouse.getWarehouseName());
        }

        List<WarehouseLineStoreDO> relations = warehouseLineStoreMapper.selectListByLineId(line.getLineId());
        List<String> storeIds = relations.stream().sorted(Comparator.comparing(WarehouseLineStoreDO::getSortNo).thenComparing(WarehouseLineStoreDO::getId)).map(WarehouseLineStoreDO::getStoreId).collect(Collectors.toList());
        respVO.setStoreIds(storeIds);
        respVO.setStoreCount(storeIds.size());
        return respVO;
    }

    private void createLineFromImport(WarehouseLineImportExcelVO row) {
        if (row.getLineName() == null || row.getLineName().isBlank() || row.getOrderWeekdays() == null || row.getOrderWeekdays().isBlank() || row.getLineStatus() == null) {
            throw new IllegalArgumentException("新建线路时，线路名称、下单星期、线路状态不能为空");
        }
        String normalizedWeekdays = normalizeWeekdays(parseWeekdays(row.getOrderWeekdays()));
        List<String> storeIds = hasStoreId(row) ? Collections.singletonList(row.getStoreId()) : Collections.emptyList();
        validateStoresEligible(row.getWarehouseId(), storeIds);
        validateSameWarehouseWeekdayConflict(null, row.getWarehouseId(), row.getLineStatus(), storeIds, parseWeekdays(normalizedWeekdays));

        WarehouseLineDO line = new WarehouseLineDO();
        line.setWarehouseId(row.getWarehouseId());
        line.setLineCode(row.getLineCode());
        line.setLineName(row.getLineName());
        line.setOrderWeekdays(normalizedWeekdays);
        line.setLineStatus(row.getLineStatus());
        line.setRemark(row.getRemark());
        warehouseLineMapper.insert(line);
        bindStoreIfAbsent(line.getLineId(), row.getStoreId(), row.getSortNo(), Collections.emptyList());
    }

    private boolean hasStoreId(WarehouseLineImportExcelVO row) {
        return row.getStoreId() != null && !row.getStoreId().isBlank();
    }

    private void bindStoreIfAbsent(Long lineId, String storeId, Integer sortNo, List<WarehouseLineStoreDO> existingRelations) {
        if (storeId == null || storeId.isBlank()) {
            return;
        }
        boolean exists = existingRelations.stream().anyMatch(item -> storeId.equals(item.getStoreId()));
        if (exists) {
            return;
        }
        WarehouseLineStoreDO relation = new WarehouseLineStoreDO();
        relation.setLineId(lineId);
        relation.setStoreId(storeId);
        relation.setSortNo(sortNo != null ? sortNo : existingRelations.size() + 1);
        warehouseLineStoreMapper.insert(relation);
    }
}
