package com.dudi.chiadata.dto;

import java.util.Map;

public class EmployeeDashboardDto {
    private long totalAssigned;
    private long untreatedCount;
    private long processingCount;
    private long completedCount;
    private Map<String, Long> statusCounts;

    public EmployeeDashboardDto() {
    }

    public long getTotalAssigned() {
        return totalAssigned;
    }

    public void setTotalAssigned(long totalAssigned) {
        this.totalAssigned = totalAssigned;
    }

    public long getUntreatedCount() {
        return untreatedCount;
    }

    public void setUntreatedCount(long untreatedCount) {
        this.untreatedCount = untreatedCount;
    }

    public long getProcessingCount() {
        return processingCount;
    }

    public void setProcessingCount(long processingCount) {
        this.processingCount = processingCount;
    }

    public long getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(long completedCount) {
        this.completedCount = completedCount;
    }

    public Map<String, Long> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(Map<String, Long> statusCounts) {
        this.statusCounts = statusCounts;
    }
}
