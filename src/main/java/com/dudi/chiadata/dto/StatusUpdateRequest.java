package com.dudi.chiadata.dto;

import jakarta.validation.constraints.NotBlank;

public class StatusUpdateRequest {
    @NotBlank(message = "Trạng thái không được để trống")
    private String status;

    public StatusUpdateRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
