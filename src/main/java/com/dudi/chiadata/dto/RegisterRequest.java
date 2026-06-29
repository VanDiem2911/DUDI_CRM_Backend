package com.dudi.chiadata.dto;

import com.dudi.chiadata.model.EmployeeProfile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.List;

public class RegisterRequest {
    @Size(min = 3, max = 20, message = "Tên đăng nhập từ 3 đến 20 ký tự")
    private String username;

    @Size(min = 4, max = 40, message = "Mật khẩu từ 4 đến 40 ký tự")
    private String password;

    private String fullName;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String phone;
    private String role;
    private String name;
    private String empName;
    private String avatarUrl;
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
    private EmployeeProfile.Address currentAddress;
    private EmployeeProfile.Address hometown;
    private List<String> galleryImages;
    private List<EmployeeProfile.WorkHistory> workHistory;

    public RegisterRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public EmployeeProfile.Address getCurrentAddress() {
        return currentAddress;
    }

    public void setCurrentAddress(EmployeeProfile.Address currentAddress) {
        this.currentAddress = currentAddress;
    }

    public EmployeeProfile.Address getHometown() {
        return hometown;
    }

    public void setHometown(EmployeeProfile.Address hometown) {
        this.hometown = hometown;
    }

    public List<String> getGalleryImages() {
        return galleryImages;
    }

    public void setGalleryImages(List<String> galleryImages) {
        this.galleryImages = galleryImages;
    }

    public List<EmployeeProfile.WorkHistory> getWorkHistory() {
        return workHistory;
    }

    public void setWorkHistory(List<EmployeeProfile.WorkHistory> workHistory) {
        this.workHistory = workHistory;
    }
}
