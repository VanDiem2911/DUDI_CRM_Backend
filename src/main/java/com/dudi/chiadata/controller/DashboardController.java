package com.dudi.chiadata.controller;

import com.dudi.chiadata.dto.AdminDashboardDto;
import com.dudi.chiadata.dto.EmployeeDashboardDto;
import com.dudi.chiadata.model.DataRecord;
import com.dudi.chiadata.model.Role;
import com.dudi.chiadata.model.User;
import com.dudi.chiadata.repository.DataRecordRepository;
import com.dudi.chiadata.repository.UserRepository;
import com.dudi.chiadata.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    DataRecordRepository dataRecordRepository;

    @Autowired
    UserRepository userRepository;

    private static final List<String> STATUSES = List.of(
            "Chưa xử lý", "Chặn người lạ", "Đã gửi tin nhắn", "Không có Zalo", "Trả lời"
    );

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardDto> getAdminDashboard() {
        AdminDashboardDto dto = new AdminDashboardDto();
        
        List<DataRecord> allRecords = dataRecordRepository.findAll();
        List<User> employees = userRepository.findByRole(Role.ROLE_EMPLOYEE);
        
        dto.setTotalData(allRecords.size());
        dto.setTotalEmployees(employees.size());
        
        long unassigned = 0;
        long assigned = 0;
        long processing = 0;
        long completed = 0;
        
        Map<String, Long> statusCounts = new HashMap<>();
        for (String status : STATUSES) {
            statusCounts.put(status, 0L);
        }
        
        for (DataRecord record : allRecords) {
            if (record.getAssignedTo() == null || record.getAssignedTo().trim().isEmpty()) {
                unassigned++;
            } else {
                assigned++;
            }
            
            String status = record.getStatus();
            if (status == null || status.trim().isEmpty()) {
                status = "Chưa xử lý";
            }
            statusCounts.put(status, statusCounts.getOrDefault(status, 0L) + 1);
            
            if ("Đã gửi tin nhắn".equals(status)) {
                processing++;
            } else if ("Trả lời".equals(status)) {
                completed++;
            }
        }
        
        dto.setUnassignedData(unassigned);
        dto.setAssignedData(assigned);
        dto.setProcessingData(processing);
        dto.setCompletedData(completed);
        dto.setStatusCounts(statusCounts);
        
        List<AdminDashboardDto.EmployeeDataProgress> progressList = new ArrayList<>();
        for (User emp : employees) {
            AdminDashboardDto.EmployeeDataProgress ep = new AdminDashboardDto.EmployeeDataProgress();
            ep.setEmployeeId(emp.getId());
            ep.setEmployeeName(emp.getFullName());
            
            long empTotal = 0;
            long empCompleted = 0;
            long empProcessing = 0;
            
            for (DataRecord record : allRecords) {
                if (emp.getId().equals(record.getAssignedTo())) {
                    empTotal++;
                    if ("Trả lời".equals(record.getStatus())) {
                        empCompleted++;
                    } else if ("Đã gửi tin nhắn".equals(record.getStatus())) {
                        empProcessing++;
                    }
                }
            }
            
            ep.setTotalAssigned(empTotal);
            ep.setCompletedCount(empCompleted);
            ep.setProcessingCount(empProcessing);
            
            progressList.add(ep);
        }
        dto.setEmployeeProgress(progressList);
        
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/employee")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<EmployeeDashboardDto> getEmployeeDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        String employeeId = userDetails.getId();
        
        List<DataRecord> empRecords = dataRecordRepository.findByAssignedTo(employeeId);
        
        EmployeeDashboardDto dto = new EmployeeDashboardDto();
        dto.setTotalAssigned(empRecords.size());
        
        long untreated = 0;
        long processing = 0;
        long completed = 0;
        
        Map<String, Long> statusCounts = new HashMap<>();
        for (String status : STATUSES) {
            statusCounts.put(status, 0L);
        }
        
        for (DataRecord record : empRecords) {
            String status = record.getStatus();
            if (status == null || status.trim().isEmpty()) {
                status = "Chưa xử lý";
            }
            statusCounts.put(status, statusCounts.getOrDefault(status, 0L) + 1);
            
            if ("Chưa xử lý".equals(status)) {
                untreated++;
            } else if ("Đã gửi tin nhắn".equals(status)) {
                processing++;
            } else if ("Trả lời".equals(status)) {
                completed++;
            }
        }
        
        dto.setUntreatedCount(untreated);
        dto.setProcessingCount(processing);
        dto.setCompletedCount(completed);
        dto.setStatusCounts(statusCounts);
        
        return ResponseEntity.ok(dto);
    }
}
