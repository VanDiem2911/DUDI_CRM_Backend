package com.dudi.chiadata.config;

import com.dudi.chiadata.model.DataRecord;
import com.dudi.chiadata.model.Role;
import com.dudi.chiadata.model.User;
import com.dudi.chiadata.repository.DataRecordRepository;
import com.dudi.chiadata.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    UserRepository userRepository;

    @Autowired
    DataRecordRepository dataRecordRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername("admin").isPresent()) {
            System.out.println(">>> Database already initialized. Skipping DataInitializer.");
            return;
        }

        // 1. Initializing default admin user if none exists
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Quản trị viên DuDi");
            admin.setEmail("admin@dudi.vn");
            admin.setPhone("0999888777");
            admin.setRole(Role.ROLE_ADMIN);
            admin.setActive(true);
            admin.setCreatedAt(Instant.now());
            admin.setUpdatedAt(Instant.now());

            userRepository.save(admin);
            System.out.println(">>> Đã khởi tạo tài khoản Admin mặc định: admin / admin123");
        }

        // 2. Initializing a default employee for quick testing
        User empObj = null;
        if (userRepository.findByUsername("nv1").isEmpty()) {
            User emp = new User();
            emp.setId("nv1");
            emp.setEmployeeId("nv1");
            emp.setUsername("nv1");
            emp.setPassword(passwordEncoder.encode("123456"));
            emp.setFullName("Nguyễn Văn Nhân Viên 1");
            emp.setEmail("nhanvien1@dudi.vn");
            emp.setPhone("0901234567");
            emp.setRole(Role.ROLE_EMPLOYEE);
            emp.setActive(true);
            emp.setCreatedAt(Instant.now());
            emp.setUpdatedAt(Instant.now());

            empObj = userRepository.save(emp);
            System.out.println(">>> Đã khởi tạo tài khoản Nhân viên mẫu: nv1 / 123456");
        } else {
            empObj = userRepository.findByUsername("nv1").orElse(null);
        }

        // 3. Initializing sample data records if none exist
        if (dataRecordRepository.count() == 0) {
            DataRecord r1 = new DataRecord();
            r1.setBusinessName("Văn phòng cho thuê quận 5 - Office Saigon");
            r1.setAddress("86 Đ. Tản Đà");
            r1.setArea("Chợ Lớn, Hồ Chí Minh");
            r1.setPhone("+84 987 110 011");
            r1.setWebsite("https://www.officesaigon.vn/van-phong-cho-thue-quan-5.html");
            r1.setBusinessType("Đại lý cho thuê văn phòng");
            r1.setGoogleMapUrl(
                    "https://www.google.com/maps/search/?api=1&query=V%C4%83n%20ph%C3%B2ng%20cho%20thu%C3%AA%20qu%E1%BA%ADn%205%20-%20Office%20Saigon");
            r1.setStatus("Chưa xử lý");
            r1.setCreatedAt(Instant.now());
            r1.setUpdatedAt(Instant.now());

            DataRecord r2 = new DataRecord();
            r2.setBusinessName("Cà phê Trung Nguyên Legend Quận 1");
            r2.setAddress("12 Đ. Alexandre de Rhodes");
            r2.setArea("Quận 1, Hồ Chí Minh");
            r2.setPhone("+84 28 3825 8585");
            r2.setWebsite("https://trungnguyenlegend.com");
            r2.setBusinessType("Quán cà phê");
            r2.setGoogleMapUrl(
                    "https://www.google.com/maps/search/?api=1&query=C%C3%A0%20ph%C3%AA%20Trung%20Nguy%C3%AAn%20Legend%20Qu%E1%BA%ADn%201");
            r2.setStatus("Đã gửi tin nhắn");
            if (empObj != null) {
                r2.setAssignedTo(empObj.getId());
                r2.setAssignedToName(empObj.getFullName());
            }
            r2.setCreatedAt(Instant.now());
            r2.setUpdatedAt(Instant.now());

            DataRecord r3 = new DataRecord();
            r3.setBusinessName("Khách sạn Majestic Sài Gòn");
            r3.setAddress("1 Đ. Đồng Khởi");
            r3.setArea("Quận 1, Hồ Chí Minh");
            r3.setPhone("+84 28 3829 5517");
            r3.setWebsite("https://majesticsaigon.com");
            r3.setBusinessType("Khách sạn");
            r3.setGoogleMapUrl(
                    "https://www.google.com/maps/search/?api=1&query=Kh%C3%A1ch%20s%E1%BA%A1n%20Majestic%20S%C3%A0i%20G%C3%B2n");
            r3.setStatus("Trả lời");
            if (empObj != null) {
                r3.setAssignedTo(empObj.getId());
                r3.setAssignedToName(empObj.getFullName());
            }
            r3.setCreatedAt(Instant.now());
            r3.setUpdatedAt(Instant.now());

            DataRecord r4 = new DataRecord();
            r4.setBusinessName("Bệnh viện Đa khoa Tâm Anh");
            r4.setAddress("2B Đ. Phổ Quang");
            r4.setArea("Tân Bình, Hồ Chí Minh");
            r4.setPhone("+84 287 102 6789");
            r4.setWebsite("https://tamanhhospital.vn");
            r4.setBusinessType("Bệnh viện tư nhân");
            r4.setGoogleMapUrl(
                    "https://www.google.com/maps/search/?api=1&query=B%E1%BB%87nh%20vi%E1%BB%87n%20%C4%90a%20khoa%20T%C3%A2m%20Anh");
            r4.setStatus("Chưa xử lý");
            r4.setCreatedAt(Instant.now());
            r4.setUpdatedAt(Instant.now());

            dataRecordRepository.saveAll(Arrays.asList(r1, r2, r3, r4));
            System.out.println(">>> Đã khởi tạo 4 bản ghi dữ liệu khách hàng mẫu!");
        }

        // Migrate existing records to new statuses
        List<DataRecord> allRecords = dataRecordRepository.findAll();
        boolean changed = false;
        for (DataRecord r : allRecords) {
            String status = r.getStatus();

            String newStatus = null;
            if ("Đang xử lý".equals(status) || "Đã liên hệ".equals(status)) {
                newStatus = "Đã gửi tin nhắn";
            } else if ("Đã hoàn thành".equals(status) || "Có tiềm năng".equals(status)) {
                newStatus = "Trả lời";
            } else if (status != null && !"Chưa xử lý".equals(status) && !"Chặn người lạ".equals(status)
                    && !"Đã gửi tin nhắn".equals(status) && !"Không có Zalo".equals(status)
                    && !"Trả lời".equals(status)) {
                newStatus = "Chưa xử lý";
            }

            if (newStatus != null) {
                r.setStatus(newStatus);
                r.setUpdatedAt(Instant.now());
                changed = true;
            }
        }
        if (changed) {
            dataRecordRepository.saveAll(allRecords);
            System.out.println(">>> Đã cập nhật trạng thái dữ liệu cũ sang bộ trạng thái mới!");
        }
    }
}
