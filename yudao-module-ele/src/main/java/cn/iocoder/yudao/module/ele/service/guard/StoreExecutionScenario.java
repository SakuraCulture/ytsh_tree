package cn.iocoder.yudao.module.ele.service.guard;

public enum StoreExecutionScenario {

    STORE_GOODS("门店商品执行", 5, 30),
    STORE_INVENTORY("门店库存执行", 5, 30);

    private final String description;
    private final int waitSeconds;
    private final int leaseMinutes;

    StoreExecutionScenario(String description, int waitSeconds, int leaseMinutes) {
        this.description = description;
        this.waitSeconds = waitSeconds;
        this.leaseMinutes = leaseMinutes;
    }

    public String getDescription() {
        return description;
    }

    public int getWaitSeconds() {
        return waitSeconds;
    }

    public int getLeaseMinutes() {
        return leaseMinutes;
    }
}
