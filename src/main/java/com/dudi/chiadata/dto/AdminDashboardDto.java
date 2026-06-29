package com.dudi.chiadata.dto;

import java.util.Map;
import java.util.List;

public class AdminDashboardDto {
    private long totalData;
    private long unassignedData;
    private long assignedData;
    private long processingData;
    private long completedData;
    private long totalEmployees;
    private Map<String, Long> statusCounts;
    private List<EmployeeDataProgress> employeeProgress;

    public AdminDashboardDto() {
    }

    public long getTotalData() {
        return totalData;
    }

    public void setTotalData(long totalData) {
        this.totalData = totalData;
    }

    public long getUnassignedData() {
        return unassignedData;
    }

    public void setUnassignedData(long unassignedData) {
        this.unassignedData = unassignedData;
    }

    public long getAssignedData() {
        return assignedData;
    }

    public void setAssignedData(long assignedData) {
        this.assignedData = assignedData;
    }

    public long getProcessingData() {
        return processingData;
    }

    public void setProcessingData(long processingData) {
        this.processingData = processingData;
    }

    public long getCompletedData() {
        return completedData;
    }

    public void setCompletedData(long completedData) {
        this.completedData = completedData;
    }

    public long getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(long totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public Map<String, Long> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(Map<String, Long> statusCounts) {
        this.statusCounts = statusCounts;
    }

    public List<EmployeeDataProgress> getEmployeeProgress() {
        return employeeProgress;
    }

    public void setEmployeeProgress(List<EmployeeDataProgress> employeeProgress) {
        this.employeeProgress = employeeProgress;
    }

    public static class EmployeeDataProgress {
        private String employeeId;
        private String employeeName;
        private long totalAssigned;
        private long completedCount;
        private long processingCount;

        public EmployeeDataProgress() {
        }

        public String getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(String employeeId) {
            this.employeeId = employeeId;
        }

        public String getEmployeeName() {
            return employeeName;
        }

        public void setEmployeeName(String employeeName) {
            this.employeeName = employeeName;
        }

        public long getTotalAssigned() {
            return totalAssigned;
        }

        public void setTotalAssigned(long totalAssigned) {
            this.totalAssigned = totalAssigned;
        }

        public long getCompletedCount() {
            return completedCount;
        }

        public void setCompletedCount(long completedCount) {
            this.completedCount = completedCount;
        }

        public long getProcessingCount() {
            return processingCount;
        }

        public void setProcessingCount(long processingCount) {
            this.processingCount = processingCount;
        }
    }
}
