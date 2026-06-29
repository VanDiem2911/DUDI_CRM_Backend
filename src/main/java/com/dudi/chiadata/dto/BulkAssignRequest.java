package com.dudi.chiadata.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class BulkAssignRequest {
    @NotEmpty(message = "Danh sách ID dữ liệu không được rỗng")
    private List<String> dataIds;

    @NotBlank(message = "ID nhân viên không được để trống")
    private String employeeId;

    public BulkAssignRequest() {
    }

    public List<String> getDataIds() {
        return dataIds;
    }

    public void setDataIds(List<String> dataIds) {
        this.dataIds = dataIds;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
}
