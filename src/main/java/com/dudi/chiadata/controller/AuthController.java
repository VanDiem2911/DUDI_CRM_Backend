package com.dudi.chiadata.controller;

import com.dudi.chiadata.dto.JwtResponse;
import com.dudi.chiadata.dto.LoginRequest;
import com.dudi.chiadata.dto.MessageResponse;
import com.dudi.chiadata.model.User;
import com.dudi.chiadata.repository.UserRepository;
import com.dudi.chiadata.security.JwtUtils;
import com.dudi.chiadata.security.UserDetailsImpl;
import com.dudi.chiadata.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getFullName(),
                role
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).body(new MessageResponse("Chưa đăng nhập"));
        }
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(new MessageResponse("Không tìm thấy người dùng"));
        }
        
        return ResponseEntity.ok(user);
    }

    @GetMapping("/lookup")
    public ResponseEntity<?> lookupEmployeeId(@RequestParam("query") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Vui lòng nhập tên hoặc số điện thoại"));
        }
        
        String cleanQuery = query.trim();
        // Try searching by phone first
        java.util.List<User> usersByPhone = userRepository.findByRoleAndPhone(Role.ROLE_EMPLOYEE, cleanQuery);
        if (!usersByPhone.isEmpty()) {
            return ResponseEntity.ok(usersByPhone.stream().map(u -> Map.of(
                "fullName", u.getFullName(),
                "username", u.getUsername()
            )).collect(Collectors.toList()));
        }
        
        // If not found, search by name
        java.util.List<User> usersByName = userRepository.findByRoleAndFullNameContainingIgnoreCase(Role.ROLE_EMPLOYEE, cleanQuery);
        return ResponseEntity.ok(usersByName.stream().map(u -> Map.of(
            "fullName", u.getFullName(),
            "username", u.getUsername()
        )).collect(Collectors.toList()));
    }
}
