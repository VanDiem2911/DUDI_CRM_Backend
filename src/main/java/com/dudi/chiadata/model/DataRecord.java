package com.dudi.chiadata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "data_records")
public class DataRecord {
    @Id
    private String id;
    private String businessName;
    private String address;
    private String area;
    private String phone;
    private String website;
    private String businessType;
    private String googleMapUrl;
    private String status = "Chưa xử lý";
    private String assignedTo;
    private String assignedToName;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public DataRecord() {
    }

    public DataRecord(String id, String businessName, String address, String area, String phone, String website, String businessType, String googleMapUrl, String status, String assignedTo, String assignedToName, String createdBy, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.businessName = businessName;
        this.address = address;
        this.area = area;
        this.phone = phone;
        this.website = website;
        this.businessType = businessType;
        this.googleMapUrl = googleMapUrl;
        this.status = status;
        this.assignedTo = assignedTo;
        this.assignedToName = assignedToName;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getGoogleMapUrl() {
        return googleMapUrl;
    }

    public void setGoogleMapUrl(String googleMapUrl) {
        this.googleMapUrl = googleMapUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getAssignedToName() {
        return assignedToName;
    }

    public void setAssignedToName(String assignedToName) {
        this.assignedToName = assignedToName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
