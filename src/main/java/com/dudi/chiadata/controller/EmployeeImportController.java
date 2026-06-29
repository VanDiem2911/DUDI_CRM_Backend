package com.dudi.chiadata.controller;

import com.dudi.chiadata.model.User;
import com.dudi.chiadata.model.EmployeeProfile;
import com.dudi.chiadata.model.Role;
import com.dudi.chiadata.repository.EmployeeProfileRepository;
import com.dudi.chiadata.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/employees")
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeImportController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @PostMapping("/import-csv")
    public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File rỗng"));
        }

        String originalFilename = file.getOriginalFilename();
        boolean isExcel = originalFilename != null && (originalFilename.endsWith(".xlsx") || originalFilename.endsWith(".xls"));
        if (isExcel) {
            return importEmployeeExcel(file);
        }

        boolean isJson = originalFilename != null && originalFilename.endsWith(".json");
        if (isJson) {
            return importEmployeeJson(file);
        }

        try {
            byte[] bytes = file.getBytes();
            java.nio.charset.Charset charset = StandardCharsets.UTF_8;
            int offset = 0;
            if (bytes.length >= 2) {
                if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
                    charset = java.nio.charset.Charset.forName("UTF-16LE");
                    offset = 2;
                } else if (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
                    charset = java.nio.charset.Charset.forName("UTF-16BE");
                    offset = 2;
                } else if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                    charset = StandardCharsets.UTF_8;
                    offset = 3;
                }
            }

            String fileContent = new String(bytes, offset, bytes.length - offset, charset);
            try (BufferedReader br = new BufferedReader(new java.io.StringReader(fileContent))) {
                String firstLine = br.readLine();
                if (firstLine == null) {
                    return ResponseEntity.badRequest().body(Map.of("message", "File không có dữ liệu"));
                }
                if (firstLine.startsWith("\uFEFF")) {
                    firstLine = firstLine.substring(1);
                }

                char separator = ',';
                int commas = 0;
                int semicolons = 0;
                int tabs = 0;
                for (int i = 0; i < firstLine.length(); i++) {
                    char c = firstLine.charAt(i);
                    if (c == ',') commas++;
                    else if (c == ';') semicolons++;
                    else if (c == '\t') tabs++;
                }
                if (semicolons > commas && semicolons > tabs) {
                    separator = ';';
                } else if (tabs > commas && tabs > semicolons) {
                    separator = '\t';
                }

                List<String> headers = parseCsvLine(firstLine, separator);
                System.out.println(">>> [DEBUG] Employee CSV firstLine: " + firstLine);
                System.out.println(">>> [DEBUG] Employee CSV detected separator: '" + separator + "'");
                System.out.println(">>> [DEBUG] Employee CSV parsed headers (size=" + headers.size() + "): " + headers);
            int nameIdx = getColumnIndex(headers, "Họ tên", "Họ và tên", "Ho ten", "Name", "Full Name", "fullName");
            int emailIdx = getColumnIndex(headers, "Email", "email");
            int userIdx = getColumnIndex(headers, "Username", "username", "Tên đăng nhập", "Ten dang nhap");
            int phoneIdx = getColumnIndex(headers, "Số điện thoại", "Số ĐT", "SĐT", "SDT", "Phone", "phone");
            int deptIdx = getColumnIndex(headers, "Phòng Ban", "Phòng ban", "Phong ban", "Department", "department");
            int passIdx = getColumnIndex(headers, "Mật khẩu", "Mat khau", "Password", "password");

            boolean hasHeader = true;
            if (nameIdx == -1 || emailIdx == -1 || userIdx == -1 || deptIdx == -1 || passIdx == -1) {
                if (headers.size() >= 5) {
                    hasHeader = false;
                    nameIdx = 0;
                    emailIdx = 1;
                    userIdx = 2;
                    phoneIdx = headers.size() > 5 ? 3 : -1;
                    deptIdx = headers.size() > 5 ? 4 : 3;
                    passIdx = headers.size() > 5 ? 5 : 4;
                } else {
                    return ResponseEntity.badRequest().body(Map.of("message", "File mẫu không đúng định dạng cột yêu cầu. Hãy tải file mẫu để kiểm tra."));
                }
            }

            int totalRows = 0;
            int successCount = 0;
            int skippedNotMarketing = 0;
            int failedCount = 0;
            List<Map<String, Object>> errors = new ArrayList<>();

            String line = firstLine;
            int rowNum = hasHeader ? 1 : 0;
            boolean isFirstProcess = !hasHeader;

            while (isFirstProcess || (line = br.readLine()) != null) {
                if (isFirstProcess) {
                    isFirstProcess = false;
                }
                rowNum++;
                List<String> columns = parseCsvLine(line, separator);
                if (columns.isEmpty() || (columns.size() == 1 && columns.get(0).isEmpty())) {
                    continue;
                }
                
                totalRows++;

                int maxIdx = Math.max(Math.max(Math.max(nameIdx, emailIdx), Math.max(userIdx, deptIdx)), Math.max(phoneIdx, passIdx));
                if (columns.size() <= maxIdx) {
                    failedCount++;
                    errors.add(Map.of("row", rowNum, "message", "Dòng không đủ số cột dữ liệu"));
                    continue;
                }

                String fullName = columns.get(nameIdx).trim();
                String email = columns.get(emailIdx).trim();
                String username = columns.get(userIdx).trim();
                String phone = phoneIdx != -1 ? columns.get(phoneIdx).trim() : "";
                String department = columns.get(deptIdx).trim();
                String password = columns.get(passIdx).trim();

                // 1. Department filter (marketing only, case-insensitive, matching variations like "Kinh doanh – Marketing")
                if (department.isEmpty() || !department.toLowerCase().contains("marketing")) {
                    skippedNotMarketing++;
                    continue;
                }

                // 2. Required fields validation
                User existingUserByUsername = userRepository.findByUsername(username).orElse(null);
                if (fullName.isEmpty() || email.isEmpty() || username.isEmpty() || (existingUserByUsername == null && password.isEmpty())) {
                    failedCount++;
                    List<String> missing = new ArrayList<>();
                    if (fullName.isEmpty()) missing.add("họ tên");
                    if (email.isEmpty()) missing.add("email");
                    if (username.isEmpty()) missing.add("username");
                    if (existingUserByUsername == null && password.isEmpty()) missing.add("mật khẩu");
                    errors.add(Map.of("row", rowNum, "message", "Thiếu " + String.join(", ", missing)));
                    continue;
                }

                // 3. Email uniqueness validation
                if (existingUserByUsername != null) {
                    User existingUserByEmail = userRepository.findByEmail(email).orElse(null);
                    if (existingUserByEmail != null && !existingUserByEmail.getUsername().equals(username)) {
                        failedCount++;
                        errors.add(Map.of("row", rowNum, "message", "Email '" + email + "' đã được sử dụng bởi tài khoản khác"));
                        continue;
                    }
                } else {
                    if (userRepository.existsByEmail(email)) {
                        failedCount++;
                        errors.add(Map.of("row", rowNum, "message", "Email '" + email + "' đã tồn tại"));
                        continue;
                    }
                }

                try {
                    User user = existingUserByUsername;
                    if (user == null) {
                        user = new User();
                        user.setId(username);
                        user.setEmployeeId(username);
                        user.setUsername(username);
                        user.setRole(Role.ROLE_EMPLOYEE);
                        user.setActive(true);
                        user.setCreatedAt(Instant.now());
                    }
                    user.setFullName(fullName);
                    user.setEmail(email);
                    user.setPhone(phone);
                    if (!password.isEmpty()) {
                        user.setPassword(passwordEncoder.encode(password));
                    }
                    user.setDepartment(department);
                    user.setUpdatedAt(Instant.now());

                    userRepository.save(user);
                    saveEmployeeProfile(username, fullName, email, phone, department, user.getCreatedAt(), user.getUpdatedAt());
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    errors.add(Map.of("row", rowNum, "message", "Lỗi lưu database: " + e.getMessage()));
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("totalRows", totalRows);
            response.put("successCount", successCount);
            response.put("skippedNotMarketing", skippedNotMarketing);
            response.put("failedCount", failedCount);
            response.put("errors", errors);

            return ResponseEntity.ok(response);
        }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi xử lý file CSV: " + e.getMessage()));
        }
    }

    private int getColumnIndex(List<String> headers, String... possibleNames) {
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).trim();
            for (String possibleName : possibleNames) {
                if (header.equalsIgnoreCase(possibleName)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private List<String> parseCsvLine(String line, char separator) {
        List<String> result = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            return result;
        }
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == separator && !inQuotes) {
                result.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString().trim());
        return result;
    }

    private ResponseEntity<?> importEmployeeExcel(MultipartFile file) {
        try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream())) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() == 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "File không có dữ liệu"));
            }

            org.apache.poi.ss.usermodel.Row firstRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < firstRow.getLastCellNum(); i++) {
                org.apache.poi.ss.usermodel.Cell cell = firstRow.getCell(i);
                headers.add(getCellValueAsString(cell));
            }

            int nameIdx = getColumnIndex(headers, "Họ tên", "Họ và tên", "Ho ten", "Name", "Full Name", "fullName");
            int emailIdx = getColumnIndex(headers, "Email", "email");
            int userIdx = getColumnIndex(headers, "Username", "username", "Tên đăng nhập", "Ten dang nhap");
            int phoneIdx = getColumnIndex(headers, "Số điện thoại", "Số ĐT", "SĐT", "SDT", "Phone", "phone");
            int deptIdx = getColumnIndex(headers, "Phòng Ban", "Phòng ban", "Phong ban", "Department", "department");
            int passIdx = getColumnIndex(headers, "Mật khẩu", "Mat khau", "Password", "password");

            boolean hasHeader = true;
            if (nameIdx == -1 || emailIdx == -1 || userIdx == -1 || deptIdx == -1 || passIdx == -1) {
                if (headers.size() >= 5) {
                    hasHeader = false;
                    nameIdx = 0;
                    emailIdx = 1;
                    userIdx = 2;
                    phoneIdx = headers.size() > 5 ? 3 : -1;
                    deptIdx = headers.size() > 5 ? 4 : 3;
                    passIdx = headers.size() > 5 ? 5 : 4;
                } else {
                    return ResponseEntity.badRequest().body(Map.of("message", "File mẫu không đúng định dạng cột yêu cầu. Hãy tải file mẫu để kiểm tra."));
                }
            }

            int totalRows = 0;
            int successCount = 0;
            int skippedNotMarketing = 0;
            int failedCount = 0;
            List<Map<String, Object>> errors = new ArrayList<>();

            int startRow = hasHeader ? 1 : 0;
            for (int rNum = startRow; rNum <= sheet.getLastRowNum(); rNum++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(rNum);
                if (row == null) continue;

                boolean isEmpty = true;
                List<String> columns = new ArrayList<>();
                for (int i = 0; i < Math.max(6, row.getLastCellNum()); i++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(i);
                    String val = getCellValueAsString(cell);
                    columns.add(val);
                    if (!val.trim().isEmpty()) {
                        isEmpty = false;
                    }
                }
                if (isEmpty) continue;

                totalRows++;

                int maxIdx = Math.max(Math.max(Math.max(nameIdx, emailIdx), Math.max(userIdx, deptIdx)), Math.max(phoneIdx, passIdx));
                if (columns.size() <= maxIdx) {
                    failedCount++;
                    errors.add(Map.of("row", rNum + 1, "message", "Dòng không đủ số cột dữ liệu"));
                    continue;
                }

                String fullName = columns.get(nameIdx).trim();
                String email = columns.get(emailIdx).trim();
                String username = columns.get(userIdx).trim();
                String phone = phoneIdx != -1 ? columns.get(phoneIdx).trim() : "";
                String department = columns.get(deptIdx).trim();
                String password = columns.get(passIdx).trim();

                if (department.isEmpty() || !department.toLowerCase().contains("marketing")) {
                    skippedNotMarketing++;
                    continue;
                }

                User existingUserByUsername = userRepository.findByUsername(username).orElse(null);
                if (fullName.isEmpty() || email.isEmpty() || username.isEmpty() || (existingUserByUsername == null && password.isEmpty())) {
                    failedCount++;
                    List<String> missing = new ArrayList<>();
                    if (fullName.isEmpty()) missing.add("họ tên");
                    if (email.isEmpty()) missing.add("email");
                    if (username.isEmpty()) missing.add("username");
                    if (existingUserByUsername == null && password.isEmpty()) missing.add("mật khẩu");
                    errors.add(Map.of("row", rNum + 1, "message", "Thiếu " + String.join(", ", missing)));
                    continue;
                }

                if (existingUserByUsername != null) {
                    User existingUserByEmail = userRepository.findByEmail(email).orElse(null);
                    if (existingUserByEmail != null && !existingUserByEmail.getUsername().equals(username)) {
                        failedCount++;
                        errors.add(Map.of("row", rNum + 1, "message", "Email '" + email + "' đã được sử dụng bởi tài khoản khác"));
                        continue;
                    }
                } else {
                    if (userRepository.existsByEmail(email)) {
                        failedCount++;
                        errors.add(Map.of("row", rNum + 1, "message", "Email '" + email + "' đã tồn tại"));
                        continue;
                    }
                }

                try {
                    User user = existingUserByUsername;
                    if (user == null) {
                        user = new User();
                        user.setId(username);
                        user.setEmployeeId(username);
                        user.setUsername(username);
                        user.setRole(Role.ROLE_EMPLOYEE);
                        user.setActive(true);
                        user.setCreatedAt(Instant.now());
                    }
                    user.setFullName(fullName);
                    user.setEmail(email);
                    user.setPhone(phone);
                    if (!password.isEmpty()) {
                        user.setPassword(passwordEncoder.encode(password));
                    }
                    user.setDepartment(department);
                    user.setUpdatedAt(Instant.now());

                    userRepository.save(user);
                    saveEmployeeProfile(username, fullName, email, phone, department, user.getCreatedAt(), user.getUpdatedAt());
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    errors.add(Map.of("row", rNum + 1, "message", "Lỗi lưu database: " + e.getMessage()));
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("totalRows", totalRows);
            response.put("successCount", successCount);
            response.put("skippedNotMarketing", skippedNotMarketing);
            response.put("failedCount", failedCount);
            response.put("errors", errors);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi xử lý file Excel: " + e.getMessage()));
        }
    }

    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double numVal = cell.getNumericCellValue();
                if (numVal == (long) numVal) {
                    return String.format("%d", (long) numVal);
                } else {
                    return String.format("%s", numVal);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return "";
                    }
                }
            default:
                return "";
        }
    }

    @PostMapping("/import-accounts")
    public ResponseEntity<?> importAccounts(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File rỗng"));
        }
        return importEmployeeJsonAccounts(file);
    }

    private ResponseEntity<?> importEmployeeJson(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(bytes);
            
            int totalRows = 0;
            int successCount = 0;
            int skippedNotMarketing = 0;
            int failedCount = 0;
            List<Map<String, Object>> errors = new ArrayList<>();

            List<Map<String, Object>> list = new ArrayList<>();
            if (rootNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode node : rootNode) {
                    list.add(mapper.convertValue(node, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}));
                }
            } else if (rootNode.isObject()) {
                list.add(mapper.convertValue(rootNode, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}));
            }

            for (int i = 0; i < list.size(); i++) {
                int rowNum = i + 1;
                totalRows++;
                Map<String, Object> map = list.get(i);

                String id = getJsonValue(map, "id", "employeeId", "username", "Username", "Tên đăng nhập");
                String name = getJsonValue(map, "name", "empName", "fullName", "Họ tên", "Họ và tên");
                String email = getJsonValue(map, "email", "Email");
                String phone = getJsonValue(map, "phone", "Số điện thoại", "SĐT");
                String dept = getJsonValue(map, "dept", "department", "Phòng ban");
                String job = getJsonValue(map, "job", "chức vụ", "Chức vụ");
                String password = getJsonValue(map, "password", "Mật khẩu", "pass");
                if (password.isEmpty()) {
                    password = "1234";
                }

                if (dept.isEmpty() || !dept.toLowerCase().contains("marketing")) {
                    skippedNotMarketing++;
                    continue;
                }

                if (id.isEmpty()) {
                    failedCount++;
                    errors.add(Map.of("row", rowNum, "message", "Thiếu thuộc tính id / employeeId / username"));
                    continue;
                }

                if (name.isEmpty() || email.isEmpty()) {
                    failedCount++;
                    List<String> missing = new ArrayList<>();
                    if (name.isEmpty()) missing.add("họ tên");
                    if (email.isEmpty()) missing.add("email");
                    errors.add(Map.of("row", rowNum, "message", "Thiếu " + String.join(", ", missing)));
                    continue;
                }

                User existingUserByUsername = userRepository.findByUsername(id).orElse(null);
                if (existingUserByUsername != null) {
                    User existingUserByEmail = userRepository.findByEmail(email).orElse(null);
                    if (existingUserByEmail != null && !existingUserByEmail.getUsername().equals(id)) {
                        failedCount++;
                        errors.add(Map.of("row", rowNum, "message", "Email '" + email + "' đã được sử dụng bởi tài khoản khác"));
                        continue;
                    }
                } else {
                    if (userRepository.existsByEmail(email)) {
                        failedCount++;
                        errors.add(Map.of("row", rowNum, "message", "Email '" + email + "' đã tồn tại"));
                        continue;
                    }
                }

                try {
                    Instant userCreatedAt = Instant.now();
                    Object jsonCrAt = map.get("createdAt");
                    if (jsonCrAt instanceof Number) {
                        userCreatedAt = Instant.ofEpochMilli(((Number) jsonCrAt).longValue());
                    } else if (jsonCrAt instanceof String) {
                        try {
                            userCreatedAt = Instant.parse((String) jsonCrAt);
                        } catch (Exception ignored) {}
                    }

                    Instant userUpdatedAt = Instant.now();
                    Object jsonUpAt = map.get("updatedAt");
                    if (jsonUpAt instanceof Number) {
                        userUpdatedAt = Instant.ofEpochMilli(((Number) jsonUpAt).longValue());
                    } else if (jsonUpAt instanceof String) {
                        try {
                            userUpdatedAt = Instant.parse((String) jsonUpAt);
                        } catch (Exception ignored) {}
                    }

                    User user = existingUserByUsername;
                    if (user == null) {
                        user = new User();
                        user.setId(id);
                        user.setEmployeeId(id);
                        user.setUsername(id);
                        user.setRole(Role.ROLE_EMPLOYEE);
                        user.setActive(true);
                        user.setCreatedAt(userCreatedAt);
                    }
                    user.setFullName(name);
                    user.setEmail(email);
                    user.setPhone(phone);
                    if (!password.isEmpty()) {
                        user.setPassword(passwordEncoder.encode(password));
                    } else if (user.getPassword() == null || user.getPassword().isEmpty()) {
                        user.setPassword(passwordEncoder.encode("1234"));
                    }
                    user.setDepartment(dept);
                    user.setUpdatedAt(userUpdatedAt);
                    userRepository.save(user);

                    // Remove date fields from map to prevent Jackson Instant deserialization errors
                    map.remove("createdAt");
                    map.remove("updatedAt");

                    final Instant finalCreatedAt = user.getCreatedAt();
                    EmployeeProfile profile = employeeProfileRepository.findByEmployeeId(id).orElseGet(() -> {
                        EmployeeProfile p = new EmployeeProfile();
                        p.setId(id);
                        p.setEmployeeId(id);
                        p.setCreatedAt(finalCreatedAt);
                        return p;
                    });

                    // Update existing profile fields with the JSON map using Jackson readerForUpdating
                    byte[] mapBytes = mapper.writeValueAsBytes(map);
                    mapper.readerForUpdating(profile).readValue(mapBytes);

                    profile.setId(id);
                    profile.setEmployeeId(id);
                    profile.setName(name);
                    profile.setEmpName(name);
                    profile.setEmail(email);
                    profile.setPhone(phone);
                    profile.setDept(dept);
                    profile.setJob(job);
                    profile.setUpdatedAt(user.getUpdatedAt());

                    employeeProfileRepository.save(profile);
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    errors.add(Map.of("row", rowNum, "message", "Lỗi lưu database: " + e.getMessage()));
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("totalRows", totalRows);
            response.put("successCount", successCount);
            response.put("skippedNotMarketing", skippedNotMarketing);
            response.put("failedCount", failedCount);
            response.put("errors", errors);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi xử lý file JSON: " + e.getMessage()));
        }
    }

    private ResponseEntity<?> importEmployeeJsonAccounts(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(bytes);
            
            int totalRows = 0;
            int successCount = 0;
            int failedCount = 0;
            List<Map<String, Object>> errors = new ArrayList<>();

            if (!rootNode.isObject()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Định dạng JSON tài khoản không hợp lệ. Phải là một Object chứa danh sách tài khoản."));
            }

            java.util.Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = rootNode.fields();
            int idx = 0;
            while (fields.hasNext()) {
                idx++;
                totalRows++;
                Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> entry = fields.next();
                String username = entry.getKey();
                com.fasterxml.jackson.databind.JsonNode valNode = entry.getValue();

                if (!valNode.isObject()) {
                    failedCount++;
                    errors.add(Map.of("row", idx, "message", "Dữ liệu tài khoản '" + username + "' phải là một Object"));
                    continue;
                }

                String password = valNode.has("password") ? valNode.get("password").asText() : "1234";
                String roleStr = valNode.has("role") ? valNode.get("role").asText() : "employee";

                Role role = Role.ROLE_EMPLOYEE;
                if ("admin".equalsIgnoreCase(roleStr) || "ROLE_ADMIN".equalsIgnoreCase(roleStr)) {
                    role = Role.ROLE_ADMIN;
                }

                try {
                    Instant accCreatedAt = Instant.now();
                    if (valNode.has("createdAt")) {
                        com.fasterxml.jackson.databind.JsonNode crNode = valNode.get("createdAt");
                        if (crNode.isNumber()) {
                            accCreatedAt = Instant.ofEpochMilli(crNode.asLong());
                        } else if (crNode.isTextual()) {
                            try {
                                accCreatedAt = Instant.parse(crNode.asText());
                            } catch (Exception ignored) {}
                        }
                    }

                    final Instant finalAccCreatedAt = accCreatedAt;
                    User user = userRepository.findByUsername(username).orElseGet(() -> {
                        User u = new User();
                        u.setId(username);
                        u.setEmployeeId(username);
                        u.setUsername(username);
                        u.setFullName(username);
                        u.setEmail(username + "@dudi.vn");
                        u.setCreatedAt(finalAccCreatedAt);
                        return u;
                    });

                    user.setPassword(passwordEncoder.encode(password));
                    user.setRole(role);
                    user.setActive(true);
                    user.setUpdatedAt(Instant.now());
                    userRepository.save(user);

                    EmployeeProfile profile = employeeProfileRepository.findByEmployeeId(username).orElseGet(() -> {
                        EmployeeProfile p = new EmployeeProfile();
                        p.setId(username);
                        p.setEmployeeId(username);
                        p.setName(username);
                        p.setEmpName(username);
                        p.setCreatedAt(user.getCreatedAt());
                        return p;
                    });
                    profile.setUpdatedAt(user.getUpdatedAt());
                    employeeProfileRepository.save(profile);

                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    errors.add(Map.of("row", idx, "message", "Lỗi lưu tài khoản '" + username + "': " + e.getMessage()));
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("totalRows", totalRows);
            response.put("successCount", successCount);
            response.put("failedCount", failedCount);
            response.put("errors", errors);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi xử lý file JSON: " + e.getMessage()));
        }
    }

    private String getJsonValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                Object val = map.get(key);
                return val != null ? val.toString().trim() : "";
            }
        }
        return "";
    }

    private void saveEmployeeProfile(String employeeId, String fullName, String email, String phone, String department, Instant createdAt, Instant updatedAt) {
        EmployeeProfile profile = employeeProfileRepository.findByEmployeeId(employeeId).orElseGet(EmployeeProfile::new);
        profile.setId(employeeId);
        profile.setEmployeeId(employeeId);
        profile.setName(fullName);
        profile.setEmpName(fullName);
        profile.setEmail(email);
        profile.setPhone(phone);
        profile.setDept(department);
        profile.setCreatedAt(profile.getCreatedAt() == null ? createdAt : profile.getCreatedAt());
        profile.setUpdatedAt(updatedAt);
        employeeProfileRepository.save(profile);
    }
}
