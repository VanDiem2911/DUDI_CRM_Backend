package com.dudi.chiadata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "employee_profiles")
public class EmployeeProfile {
    @Id
    private String id;
    private String employeeId;
    private String name;
    private String empName;
    private String avatarUrl;
    private String phone;
    private String email;
    private String gender;
    private String dob;
    private String cccd;
    private String cccdIssueDate;
    private String cccdIssuePlace;
    private String dept;
    private String job;
    private String contractType;
    private String status;
    private String start;
    private String endIntern;
    private String resignDate;
    private String university;
    private String bankName;
    private String bankAccount;
    private String note;
    private Address currentAddress;
    private Address hometown;
    private List<String> galleryImages = new ArrayList<>();
    private List<WorkHistory> workHistory = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public static class Address {
        private String province;
        private String district;
        private String ward;
        private String street;

        public String getProvince() {
            return province;
        }

        public void setProvince(String province) {
            this.province = province;
        }

        public String getDistrict() {
            return district;
        }

        public void setDistrict(String district) {
            this.district = district;
        }

        public String getWard() {
            return ward;
        }

        public void setWard(String ward) {
            this.ward = ward;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }
    }

    public static class WorkHistory {
        private String position;
        private String startDate;
        private String endDate;

        public String getPosition() {
            return position;
        }

        public void setPosition(String position) {
            this.position = position;
        }

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getCccd() {
        return cccd;
    }

    public void setCccd(String cccd) {
        this.cccd = cccd;
    }

    public String getCccdIssueDate() {
        return cccdIssueDate;
    }

    public void setCccdIssueDate(String cccdIssueDate) {
        this.cccdIssueDate = cccdIssueDate;
    }

    public String getCccdIssuePlace() {
        return cccdIssuePlace;
    }

    public void setCccdIssuePlace(String cccdIssuePlace) {
        this.cccdIssuePlace = cccdIssuePlace;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEndIntern() {
        return endIntern;
    }

    public void setEndIntern(String endIntern) {
        this.endIntern = endIntern;
    }

    public String getResignDate() {
        return resignDate;
    }

    public void setResignDate(String resignDate) {
        this.resignDate = resignDate;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Address getCurrentAddress() {
        return currentAddress;
    }

    public void setCurrentAddress(Address currentAddress) {
        this.currentAddress = currentAddress;
    }

    public Address getHometown() {
        return hometown;
    }

    public void setHometown(Address hometown) {
        this.hometown = hometown;
    }

    public List<String> getGalleryImages() {
        return galleryImages;
    }

    public void setGalleryImages(List<String> galleryImages) {
        this.galleryImages = galleryImages;
    }

    public List<WorkHistory> getWorkHistory() {
        return workHistory;
    }

    public void setWorkHistory(List<WorkHistory> workHistory) {
        this.workHistory = workHistory;
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
