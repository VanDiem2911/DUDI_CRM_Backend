package com.dudi.chiadata.controller;

import com.cloudinary.Cloudinary;
import com.dudi.chiadata.dto.MessageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/upload")
public class UploadController {
    @Autowired
    Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @Value("${cloudinary.folder:quanlynhansu}")
    private String folder;

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Chua cau hinh Cloudinary"));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("File anh khong duoc de trong"));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.matches("image/(jpeg|jpg|png|webp)")) {
            return ResponseEntity.badRequest().body(new MessageResponse("Chi ho tro anh jpg, jpeg, png, webp"));
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), Map.of(
                    "folder", folder,
                    "resource_type", "image"
            ));
            return ResponseEntity.ok(Map.of("url", String.valueOf(result.get("secure_url"))));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new MessageResponse("Upload anh that bai: " + e.getMessage()));
        }
    }
}
