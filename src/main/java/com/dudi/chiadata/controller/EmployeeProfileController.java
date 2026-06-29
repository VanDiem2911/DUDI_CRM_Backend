package com.dudi.chiadata.controller;

import com.dudi.chiadata.dto.ChangePasswordRequest;
import com.dudi.chiadata.dto.MessageResponse;
import com.dudi.chiadata.model.EmployeeProfile;
import com.dudi.chiadata.model.User;
import com.dudi.chiadata.repository.EmployeeProfileRepository;
import com.dudi.chiadata.repository.UserRepository;
import com.dudi.chiadata.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/employee/profile")
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeProfileController {
    @Autowired
    UserRepository userRepository;

    @Autowired
    EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<?> getMyProfile() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(404).body(new MessageResponse("Khong tim thay nhan vien"));
        }

        EmployeeProfile profile = employeeProfileRepository.findByEmployeeId(user.getId()).orElseGet(EmployeeProfile::new);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("employeeId", user.getEmployeeId() == null ? user.getId() : user.getEmployeeId());
        userInfo.put("username", user.getUsername());
        userInfo.put("fullName", user.getFullName());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("active", user.isActive());
        return ResponseEntity.ok(Map.of(
                "user", userInfo,
                "profile", profile
        ));
    }

    @PatchMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(404).body(new MessageResponse("Khong tim thay nhan vien"));
        }
        if (request.getCurrentPassword() == null || request.getNewPassword() == null || request.getConfirmPassword() == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Vui long nhap day du mat khau"));
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Mat khau hien tai khong dung"));
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Xac nhan mat khau moi khong khop"));
        }
        if (request.getNewPassword().trim().length() < 4) {
            return ResponseEntity.badRequest().body(new MessageResponse("Mat khau moi toi thieu 4 ky tu"));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("Doi mat khau thanh cong"));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            return null;
        }
        return userRepository.findById(userDetails.getId()).orElse(null);
    }
}
