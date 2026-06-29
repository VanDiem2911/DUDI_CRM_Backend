package com.dudi.chiadata.controller;

import com.dudi.chiadata.dto.MessageResponse;
import com.dudi.chiadata.dto.RegisterRequest;
import com.dudi.chiadata.dto.UserDto;
import com.dudi.chiadata.model.EmployeeProfile;
import com.dudi.chiadata.model.Role;
import com.dudi.chiadata.model.User;
import com.dudi.chiadata.repository.DataRecordRepository;
import com.dudi.chiadata.repository.EmployeeProfileRepository;
import com.dudi.chiadata.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    @Autowired
    UserRepository userRepository;

    @Autowired
    EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    DataRecordRepository dataRecordRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter PROFILE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllEmployees() {
        List<User> employees = userRepository.findByRole(Role.ROLE_EMPLOYEE);
        List<UserDto> dtos = employees.stream().map(emp -> {
            UserDto dto = toUserDto(emp);
            long count = dataRecordRepository.countByAssignedTo(emp.getId());
            dto.setAssignedDataCount(count);
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<?> createEmployee(@Valid @RequestBody RegisterRequest registerRequest) {
        if (registerRequest.getUsername() == null || registerRequest.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Ma nhan vien khong duoc de trong!"));
        }

        String validationMessage = validateEmployeeProfileRequest(registerRequest);
        if (validationMessage != null) {
            return ResponseEntity.badRequest().body(new MessageResponse(validationMessage));
        }

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Lỗi: Tên đăng nhập đã tồn tại!"));
        }

        User user = new User();
        String employeeId = registerRequest.getUsername().trim();
        user.setId(employeeId);
        user.setEmployeeId(employeeId);
        user.setUsername(employeeId);
        user.setFullName(getRequestFullName(registerRequest));
        user.setEmail(registerRequest.getEmail());
        user.setPhone(registerRequest.getPhone());
        user.setPassword(passwordEncoder.encode(coalesce(registerRequest.getPassword(), "1234")));
        user.setRole(Role.ROLE_EMPLOYEE);
        user.setActive(true);
        user.setDepartment(coalesce(registerRequest.getDept(), ""));
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        saveEmployeeProfile(user.getId(), registerRequest, user.getCreatedAt(), user.getUpdatedAt());

        return ResponseEntity.ok(new MessageResponse("Đã thêm nhân viên thành công!"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEmployee(@PathVariable String id, @RequestBody RegisterRequest updateRequest) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String validationMessage = validateEmployeeProfileRequest(updateRequest);
        if (validationMessage != null) {
            return ResponseEntity.badRequest().body(new MessageResponse(validationMessage));
        }

        if (!user.getUsername().equals(updateRequest.getUsername()) && userRepository.existsByUsername(updateRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Lỗi: Tên đăng nhập đã tồn tại!"));
        }

        user.setUsername(updateRequest.getUsername());
        user.setFullName(getRequestFullName(updateRequest));
        user.setEmail(updateRequest.getEmail());
        user.setPhone(updateRequest.getPhone());
        user.setEmployeeId(user.getId());
        user.setDepartment(coalesce(updateRequest.getDept(), user.getDepartment()));
        
        if (updateRequest.getPassword() != null && !updateRequest.getPassword().trim().isEmpty() && updateRequest.getPassword().length() >= 4) {
            user.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
        }
        
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        saveEmployeeProfile(user.getId(), updateRequest, user.getCreatedAt(), user.getUpdatedAt());

        return ResponseEntity.ok(new MessageResponse("Cập nhật thông tin nhân viên thành công!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable String id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        dataRecordRepository.findByAssignedTo(id).forEach(record -> {
            record.setAssignedTo(null);
            record.setAssignedToName(null);
            record.setUpdatedAt(Instant.now());
            dataRecordRepository.save(record);
        });

        userRepository.deleteById(id);
        employeeProfileRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("Đã xóa nhân viên thành công!"));
    }

    @PostMapping("/delete-multiple")
    public ResponseEntity<?> deleteMultipleEmployees(@RequestBody List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Danh sách ID rỗng!"));
        }
        for (String id : ids) {
            if (userRepository.existsById(id)) {
                dataRecordRepository.findByAssignedTo(id).forEach(record -> {
                    record.setAssignedTo(null);
                    record.setAssignedToName(null);
                    record.setUpdatedAt(Instant.now());
                    dataRecordRepository.save(record);
                });
                userRepository.deleteById(id);
                employeeProfileRepository.deleteById(id);
            }
        }
        return ResponseEntity.ok(new MessageResponse("Đã xóa danh sách nhân viên thành công!"));
    }

    @PatchMapping("/{id}/lock")
    public ResponseEntity<?> toggleLockUser(@PathVariable String id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setActive(!user.isActive());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        String status = user.isActive() ? "mở khóa" : "khóa";
        return ResponseEntity.ok(new MessageResponse("Đã " + status + " tài khoản nhân viên thành công!"));
    }
    private UserDto toUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmployeeId(coalesce(user.getEmployeeId(), user.getId()));
        dto.setFullName(user.getFullName());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setActive(user.isActive());
        dto.setDepartment(user.getDepartment());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        employeeProfileRepository.findByEmployeeId(user.getId()).ifPresent(profile -> {
            dto.setProfile(profile);
            dto.setFullName(coalesce(profile.getEmpName(), coalesce(profile.getName(), dto.getFullName())));
            dto.setEmail(coalesce(profile.getEmail(), dto.getEmail()));
            dto.setPhone(coalesce(profile.getPhone(), dto.getPhone()));
            dto.setDepartment(coalesce(profile.getDept(), dto.getDepartment()));
            dto.setJob(profile.getJob());
            dto.setStatus(profile.getStatus());
            dto.setAvatarUrl(profile.getAvatarUrl());
        });

        return dto;
    }

    private void saveEmployeeProfile(String employeeId, RegisterRequest request, Instant createdAt, Instant updatedAt) {
        EmployeeProfile profile = employeeProfileRepository.findByEmployeeId(employeeId).orElseGet(EmployeeProfile::new);
        profile.setId(employeeId);
        profile.setEmployeeId(employeeId);
        profile.setName(coalesce(request.getName(), getRequestFullName(request)));
        profile.setEmpName(coalesce(request.getEmpName(), getRequestFullName(request)));
        profile.setAvatarUrl(request.getAvatarUrl());
        profile.setPhone(request.getPhone());
        profile.setEmail(request.getEmail());
        profile.setGender(request.getGender());
        profile.setDob(request.getDob());
        profile.setCccd(request.getCccd());
        profile.setCccdIssueDate(request.getCccdIssueDate());
        profile.setCccdIssuePlace(request.getCccdIssuePlace());
        profile.setDept(request.getDept());
        profile.setJob(request.getJob());
        profile.setContractType(request.getContractType());
        profile.setStatus(request.getStatus());
        profile.setStart(request.getStart());
        profile.setEndIntern(request.getEndIntern());
        profile.setResignDate(request.getResignDate());
        profile.setUniversity(request.getUniversity());
        profile.setBankName(request.getBankName());
        profile.setBankAccount(request.getBankAccount());
        profile.setNote(request.getNote());
        profile.setCurrentAddress(request.getCurrentAddress());
        profile.setHometown(request.getHometown());
        profile.setGalleryImages(normalizeGalleryImages(request.getGalleryImages(), profile.getGalleryImages()));
        profile.setWorkHistory(normalizeWorkHistory(request.getWorkHistory()));
        profile.setCreatedAt(profile.getCreatedAt() == null ? createdAt : profile.getCreatedAt());
        profile.setUpdatedAt(updatedAt);
        employeeProfileRepository.save(profile);
    }

    private String coalesce(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String getRequestFullName(RegisterRequest request) {
        return coalesce(request.getFullName(), coalesce(request.getEmpName(), request.getName()));
    }

    private String validateEmployeeProfileRequest(RegisterRequest request) {
        if (hasText(request.getCccd()) && !request.getCccd().trim().matches("\\d{12}")) {
            return "CCCD phai gom dung 12 so!";
        }

        if (hasText(request.getBankAccount()) && !request.getBankAccount().trim().matches("\\d+")) {
            return "So tai khoan ngan hang chi duoc nhap so!";
        }

        LocalDate dob = parseProfileDate(request.getDob());
        LocalDate cccdIssueDate = parseProfileDate(request.getCccdIssueDate());
        LocalDate start = parseProfileDate(request.getStart());
        LocalDate today = LocalDate.now();

        if (hasText(request.getDob()) && dob == null) {
            return "Ngay sinh khong hop le!";
        }
        if (hasText(request.getCccdIssueDate()) && cccdIssueDate == null) {
            return "Ngay cap CCCD khong hop le!";
        }
        if (!hasText(request.getStart())) {
            return "Ngay bat dau khong duoc de trong!";
        }
        if (start == null) {
            return "Ngay bat dau khong hop le!";
        }
        if (dob != null && dob.isAfter(today)) {
            return "Ngay sinh khong duoc lon hon ngay hien tai!";
        }
        if (cccdIssueDate != null && cccdIssueDate.isAfter(today)) {
            return "Ngay cap CCCD khong duoc lon hon ngay hien tai!";
        }
        if (dob != null && cccdIssueDate != null && cccdIssueDate.isBefore(dob)) {
            return "Ngay cap CCCD khong duoc truoc ngay sinh!";
        }
        if (dob != null && start.isBefore(dob)) {
            return "Ngay bat dau khong duoc truoc ngay sinh!";
        }

        return null;
    }

    private LocalDate parseProfileDate(String value) {
        if (!hasText(value)) {
            return null;
        }

        String trimmed = value.trim();
        try {
            if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(trimmed);
            }
            return LocalDate.parse(trimmed, PROFILE_DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private List<String> normalizeGalleryImages(List<String> requestImages, List<String> existingImages) {
        List<String> normalized = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String requestImage = requestImages != null && requestImages.size() > i ? requestImages.get(i) : null;
            String existingImage = existingImages != null && existingImages.size() > i ? existingImages.get(i) : "";
            normalized.add(coalesce(requestImage, existingImage));
        }
        return normalized;
    }

    private List<EmployeeProfile.WorkHistory> normalizeWorkHistory(List<EmployeeProfile.WorkHistory> requestHistory) {
        EmployeeProfile.WorkHistory item = new EmployeeProfile.WorkHistory();
        if (requestHistory != null && !requestHistory.isEmpty() && requestHistory.get(0) != null) {
            EmployeeProfile.WorkHistory first = requestHistory.get(0);
            item.setPosition(coalesce(first.getPosition(), ""));
            item.setStartDate(coalesce(first.getStartDate(), ""));
            item.setEndDate("");
        } else {
            item.setPosition("");
            item.setStartDate("");
            item.setEndDate("");
        }
        return List.of(item);
    }
}
