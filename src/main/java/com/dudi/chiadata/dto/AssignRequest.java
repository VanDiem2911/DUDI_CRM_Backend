package com.dudi.chiadata.dto;

import jakarta.validation.constraints.NotBlank;

public class AssignRequest {
    @NotBlank(message = "Data ID không được để trống")
    private String dataId;

    @NotBlank(message = "Nhân viên ID không được để trống")
    private String employeeId;

    public AssignRequest() {
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
}
