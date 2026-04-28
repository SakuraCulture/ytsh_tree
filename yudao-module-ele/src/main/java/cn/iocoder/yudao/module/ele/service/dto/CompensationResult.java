package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CompensationResult {
    private int totalCount;
    private int successCount;
    private int failedCount;
    private boolean allCompleted;
    private List<CompensationTaskResult> successTasks = new ArrayList<>();
    private List<CompensationTaskResult> failedTasks = new ArrayList<>();
    
    public void addResult(CompensationTaskResult result) {
        if (result.isSuccess()) {
            successCount++;
            successTasks.add(result);
        } else {
            failedCount++;
            failedTasks.add(result);
        }
    }
    
    public boolean isAllSuccess() {
        return failedCount == 0;
    }
    
    public static CompensationResult success() {
        CompensationResult result = new CompensationResult();
        result.setAllCompleted(true);
        return result;
    }
}
