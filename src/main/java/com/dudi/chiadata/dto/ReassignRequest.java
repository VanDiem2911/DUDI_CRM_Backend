package com.dudi.chiadata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class ReassignRequest {
    @NotEmpty(message = "Danh sách ID dữ liệu không được rỗng")
    private List<String> dataIds;

    @NotBlank(message = "ID nhân viên mới không được để trống")
    private String toEmployeeId;

    public ReassignRequest() {
    }

    public List<String> getDataIds() {
        return dataIds;
    }

    public void setDataIds(List<String> dataIds) {
        this.dataIds = dataIds;
    }

    public String getToEmployeeId() {
        return toEmployeeId;
    }

    public void setToEmployeeId(String toEmployeeId) {
        this.toEmployeeId = toEmployeeId;
    }
}
