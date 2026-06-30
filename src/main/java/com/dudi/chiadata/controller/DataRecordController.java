package com.dudi.chiadata.controller;

import com.dudi.chiadata.dto.*;
import com.dudi.chiadata.model.DataRecord;
import com.dudi.chiadata.model.User;
import com.dudi.chiadata.repository.DataRecordRepository;
import com.dudi.chiadata.repository.UserRepository;
import com.dudi.chiadata.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import java.time.Instant;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import com.dudi.chiadata.model.Role;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class DataRecordController {

    @Autowired
    DataRecordRepository dataRecordRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MongoTemplate mongoTemplate;

    // ==========================================
    // ADMIN ENDPOINTS (CRUD)
    // ==========================================

    @GetMapping("/data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllData(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Query query = new Query();

        if (status != null && !status.trim().isEmpty()) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        if (assignedTo != null && !assignedTo.trim().isEmpty()) {
            if (assignedTo.equals("unassigned")) {
                query.addCriteria(new Criteria().orOperator(
                        Criteria.where("assignedTo").is(null),
                        Criteria.where("assignedTo").is("")
                ));
            } else {
                query.addCriteria(Criteria.where("assignedTo").is(assignedTo));
            }
        }

        if (area != null && !area.trim().isEmpty()) {
            query.addCriteria(Criteria.where("area").regex(area, "i"));
        }

        if (search != null && !search.trim().isEmpty()) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("businessName").regex(search, "i"),
                    Criteria.where("address").regex(search, "i"),
                    Criteria.where("phone").regex(search, "i")
            ));
        }

        long total = mongoTemplate.count(query, DataRecord.class);
        query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<DataRecord> records = mongoTemplate.find(query, DataRecord.class);

        Map<String, Object> response = new HashMap<>();
        response.put("content", records);
        response.put("totalElements", total);
        response.put("totalPages", (int) Math.ceil((double) total / size));
        response.put("number", page);
        response.put("size", size);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/data/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDataById(@PathVariable String id) {
        return dataRecordRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createData(@Valid @RequestBody DataRecordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        DataRecord record = new DataRecord();
        record.setBusinessName(request.getBusinessName());
        record.setAddress(request.getAddress());
        record.setArea(request.getArea());
        record.setPhone(request.getPhone());
        record.setWebsite(request.getWebsite());
        record.setBusinessType(request.getBusinessType());
        record.setGoogleMapUrl(request.getGoogleMapUrl());
        record.setNote(request.getNote());
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            record.setStatus(request.getStatus());
        }
        record.setCreatedBy(userDetails.getId());
        record.setCreatedAt(Instant.now());
        record.setUpdatedAt(Instant.now());

        dataRecordRepository.save(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    @PutMapping("/data/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateData(@PathVariable String id, @Valid @RequestBody DataRecordRequest request) {
        return dataRecordRepository.findById(id).map(record -> {
            record.setBusinessName(request.getBusinessName());
            record.setAddress(request.getAddress());
            record.setArea(request.getArea());
            record.setPhone(request.getPhone());
            record.setWebsite(request.getWebsite());
            record.setBusinessType(request.getBusinessType());
            record.setGoogleMapUrl(request.getGoogleMapUrl());
            record.setNote(request.getNote());
            if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
                record.setStatus(request.getStatus());
            }
            record.setUpdatedAt(Instant.now());
            dataRecordRepository.save(record);
            return ResponseEntity.ok(record);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/data/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteData(@PathVariable String id) {
        if (!dataRecordRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        dataRecordRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("Xóa data thành công!"));
    }

    @PostMapping("/data/delete-bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteDataBulk(@RequestBody List<String> ids) {
        List<DataRecord> records = dataRecordRepository.findAllById(ids);
        dataRecordRepository.deleteAll(records);
        return ResponseEntity.ok(new MessageResponse("Đã xóa " + records.size() + " data thành công!"));
    }

    // ==========================================
    // ASSIGNMENT ENDPOINTS
    // ==========================================

    @PostMapping("/assignments/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignData(@Valid @RequestBody AssignRequest request) {
        DataRecord record = dataRecordRepository.findById(request.getDataId()).orElse(null);
        if (record == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Không tìm thấy data"));
        }

        User employee = userRepository.findById(request.getEmployeeId()).orElse(null);
        if (employee == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Không tìm thấy nhân viên"));
        }

        record.setAssignedTo(employee.getId());
        record.setAssignedToName(employee.getFullName());
        record.setUpdatedAt(Instant.now());
        dataRecordRepository.save(record);

        return ResponseEntity.ok(new MessageResponse("Đã chia data cho nhân viên " + employee.getFullName() + " thành công!"));
    }

    @PostMapping("/assignments/assign-bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignDataBulk(@Valid @RequestBody BulkAssignRequest request) {
        User employee = userRepository.findById(request.getEmployeeId()).orElse(null);
        if (employee == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Không tìm thấy nhân viên"));
        }

        List<DataRecord> records = dataRecordRepository.findAllById(request.getDataIds());
        for (DataRecord record : records) {
            record.setAssignedTo(employee.getId());
            record.setAssignedToName(employee.getFullName());
            record.setUpdatedAt(Instant.now());
        }
        dataRecordRepository.saveAll(records);

        return ResponseEntity.ok(new MessageResponse("Đã chia " + records.size() + " data cho nhân viên " + employee.getFullName() + " thành công!"));
    }

    @PatchMapping("/assignments/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reassignData(@Valid @RequestBody ReassignRequest request) {
        User toEmployee = userRepository.findById(request.getToEmployeeId()).orElse(null);
        if (toEmployee == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Không tìm thấy nhân viên nhận"));
        }

        List<DataRecord> records = dataRecordRepository.findAllById(request.getDataIds());
        for (DataRecord record : records) {
            record.setAssignedTo(toEmployee.getId());
            record.setAssignedToName(toEmployee.getFullName());
            record.setUpdatedAt(Instant.now());
        }
        dataRecordRepository.saveAll(records);

        return ResponseEntity.ok(new MessageResponse("Đã chuyển giao " + records.size() + " data sang nhân viên " + toEmployee.getFullName() + " thành công!"));
    }

    // ==========================================
    // EMPLOYEE DATA ENDPOINTS
    // ==========================================

    @GetMapping("/employee/data")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> getEmployeeData(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        String employeeId = userDetails.getId();

        Query query = new Query();
        query.addCriteria(Criteria.where("assignedTo").is(employeeId));

        if (status != null && !status.trim().isEmpty()) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        if (search != null && !search.trim().isEmpty()) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("businessName").regex(search, "i"),
                    Criteria.where("address").regex(search, "i"),
                    Criteria.where("phone").regex(search, "i")
            ));
        }

        long total = mongoTemplate.count(query, DataRecord.class);
        query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        List<DataRecord> records = mongoTemplate.find(query, DataRecord.class);

        Map<String, Object> response = new HashMap<>();
        response.put("content", records);
        response.put("totalElements", total);
        response.put("totalPages", (int) Math.ceil((double) total / size));
        response.put("number", page);
        response.put("size", size);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/data/auto-assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> autoAssignData() {
        List<User> allEmployees = userRepository.findByRole(Role.ROLE_EMPLOYEE);
        List<User> targetEmployees = new ArrayList<>();
        
        for (User emp : allEmployees) {
            if (emp.isActive() && emp.getDepartment() != null) {
                String dept = emp.getDepartment().trim().toLowerCase();
                if (dept.contains("marketing")) {
                    targetEmployees.add(emp);
                }
            }
        }

        if (targetEmployees.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Không tìm thấy nhân viên đang hoạt động để chia data"));
        }

        List<DataRecord> allRecords = dataRecordRepository.findAll();
        List<DataRecord> unassignedRecords = new ArrayList<>();
        for (DataRecord r : allRecords) {
            if (r.getAssignedTo() == null || r.getAssignedTo().trim().isEmpty()) {
                unassignedRecords.add(r);
            }
        }

        int totalUnassigned = unassignedRecords.size();
        int totalEmployeesAssigned = targetEmployees.size();
        int assignedCount = 0;

        Map<String, Integer> countsMap = new HashMap<>();
        for (User emp : targetEmployees) {
            countsMap.put(emp.getId(), 0);
        }

        if (totalUnassigned > 0) {
            for (int i = 0; i < unassignedRecords.size(); i++) {
                DataRecord record = unassignedRecords.get(i);
                User employee = targetEmployees.get(i % totalEmployeesAssigned);
                
                record.setAssignedTo(employee.getId());
                record.setAssignedToName(employee.getFullName());
                record.setUpdatedAt(Instant.now());
                
                countsMap.put(employee.getId(), countsMap.get(employee.getId()) + 1);
                assignedCount++;
            }
            dataRecordRepository.saveAll(unassignedRecords);
        }

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (User emp : targetEmployees) {
            Map<String, Object> empResult = new HashMap<>();
            empResult.put("employeeId", emp.getId());
            empResult.put("employeeName", emp.getFullName());
            empResult.put("assignedCount", countsMap.getOrDefault(emp.getId(), 0));
            resultList.add(empResult);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalUnassignedData", totalUnassigned);
        response.put("totalEmployees", totalEmployeesAssigned);
        response.put("assignedCount", assignedCount);
        response.put("result", resultList);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/data/import-csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importDataCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File rỗng"));
        }

        String originalFilename = file.getOriginalFilename();
        boolean isExcel = originalFilename != null && (originalFilename.endsWith(".xlsx") || originalFilename.endsWith(".xls"));
        if (isExcel) {
            return importDataExcel(file);
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
                System.out.println(">>> [DEBUG] Import CSV firstLine: " + firstLine);
                System.out.println(">>> [DEBUG] Import CSV detected separator: '" + separator + "'");
                System.out.println(">>> [DEBUG] Import CSV parsed headers (size=" + headers.size() + "): " + headers);
            int nameIdx = getColumnIndex(headers, "Tên doanh nghiệp", "Ten doanh nghiep", "Business Name", "businessName", "Name");
            int addrIdx = getColumnIndex(headers, "Địa chỉ", "Dia chi", "Address", "address", "Đường", "Duong");
            int areaIdx = getColumnIndex(headers, "Khu vực", "Khu vuc", "Area", "area");
            int phoneIdx = getColumnIndex(headers, "Số điện thoại", "So dien thoai", "Số ĐT", "SĐT", "SDT", "Phone", "phone");
            int webIdx = getColumnIndex(headers, "Website", "website", "Trang web");
            int typeIdx = getColumnIndex(headers, "Loại hình", "Loai hinh", "Danh mục", "Danh muc", "Type", "businessType");
            int mapsIdx = getColumnIndex(headers, "Google Maps", "Google Map", "Maps", "googleMapUrl");
            int noteIdx = getColumnIndex(headers, "Ghi chú", "Ghi chu", "Note", "note");

            boolean hasHeader = true;
            if (nameIdx == -1 || phoneIdx == -1 || addrIdx == -1 || areaIdx == -1) {
                if (headers.size() >= 4) {
                    hasHeader = false;
                    nameIdx = 0;
                    addrIdx = 1;
                    areaIdx = 2;
                    phoneIdx = 3;
                    webIdx = headers.size() > 4 ? 4 : -1;
                    typeIdx = headers.size() > 5 ? 5 : -1;
                    mapsIdx = headers.size() > 6 ? 6 : -1;
                    noteIdx = headers.size() > 7 ? 7 : -1;
                } else {
                    return ResponseEntity.badRequest().body(Map.of("message", "File CSV không khớp các cột bắt buộc (Tên doanh nghiệp, Địa chỉ, Khu vực, Số điện thoại). Hãy tải file mẫu để kiểm tra."));
                }
            }

            int totalRows = 0;
            int successCount = 0;
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

                int maxIdx = Math.max(Math.max(Math.max(nameIdx, phoneIdx), Math.max(addrIdx, areaIdx)), Math.max(Math.max(webIdx, typeIdx), Math.max(mapsIdx, noteIdx)));
                if (columns.size() <= maxIdx) {
                    failedCount++;
                    errors.add(Map.of("row", rowNum, "message", "Dòng không đủ số cột dữ liệu"));
                    continue;
                }

                String businessName = columns.get(nameIdx).trim();
                String address = columns.get(addrIdx).trim();
                String area = columns.get(areaIdx).trim();
                String phone = columns.get(phoneIdx).trim();
                String website = webIdx != -1 ? columns.get(webIdx).trim() : "";
                String businessType = typeIdx != -1 ? columns.get(typeIdx).trim() : "";
                String googleMapUrl = mapsIdx != -1 ? columns.get(mapsIdx).trim() : "";
                String note = noteIdx != -1 && columns.size() > noteIdx ? columns.get(noteIdx).trim() : "";

                if (businessName.isEmpty()) {
                    failedCount++;
                    errors.add(Map.of("row", rowNum, "message", "Thiếu tên doanh nghiệp"));
                    continue;
                }

                if (!phone.isEmpty() && dataRecordRepository.existsByPhone(phone)) {
                    failedCount++;
                    errors.add(Map.of("row", rowNum, "message", "Số điện thoại đã tồn tại"));
                    continue;
                }

                try {
                    DataRecord record = new DataRecord();
                    record.setBusinessName(businessName);
                    record.setAddress(address);
                    record.setArea(area);
                    record.setPhone(phone);
                    record.setWebsite(website);
                    record.setBusinessType(businessType);
                    record.setGoogleMapUrl(googleMapUrl);
                    record.setNote(note);
                    record.setStatus("Chưa xử lý");
                    record.setAssignedTo(null);
                    record.setAssignedToName(null);
                    record.setCreatedAt(Instant.now());
                    record.setUpdatedAt(Instant.now());

                    dataRecordRepository.save(record);
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    errors.add(Map.of("row", rowNum, "message", "Lỗi lưu database: " + e.getMessage()));
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("totalRows", totalRows);
            response.put("successCount", successCount);
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

    @PatchMapping("/employee/data/{id}/status")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> updateStatusByEmployee(
            @PathVariable String id,
            @Valid @RequestBody StatusUpdateRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        DataRecord record = dataRecordRepository.findById(id).orElse(null);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }

        // Security check: Employee can only update status of their assigned data records
        if (record.getAssignedTo() == null || !record.getAssignedTo().equals(userDetails.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("Lỗi: Bạn không được phép cập nhật dữ liệu của nhân viên khác."));
        }

        record.setStatus(request.getStatus());
        record.setUpdatedAt(Instant.now());
        dataRecordRepository.save(record);

        return ResponseEntity.ok(record);
    }

    @PatchMapping("/data/{id}/note")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<?> updateNote(
            @PathVariable String id,
            @Valid @RequestBody NoteUpdateRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

        DataRecord record = dataRecordRepository.findById(id).orElse(null);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }

        // Security check: Employee can only update note of their assigned data records, Admin can update any
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            if (record.getAssignedTo() == null || !record.getAssignedTo().equals(userDetails.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new MessageResponse("Lỗi: Bạn không được phép cập nhật dữ liệu của nhân viên khác."));
            }
        }

        record.setNote(request.getNote());
        record.setUpdatedAt(Instant.now());
        dataRecordRepository.save(record);

        return ResponseEntity.ok(record);
    }

    private ResponseEntity<?> importDataExcel(MultipartFile file) {
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

            int nameIdx = getColumnIndex(headers, "Tên doanh nghiệp", "Ten doanh nghiep", "Business Name", "businessName", "Name");
            int addrIdx = getColumnIndex(headers, "Địa chỉ", "Dia chi", "Address", "address", "Đường", "Duong");
            int areaIdx = getColumnIndex(headers, "Khu vực", "Khu vuc", "Area", "area");
            int phoneIdx = getColumnIndex(headers, "Số điện thoại", "So dien thoai", "Số ĐT", "SĐT", "SDT", "Phone", "phone");
            int webIdx = getColumnIndex(headers, "Website", "website", "Trang web");
            int typeIdx = getColumnIndex(headers, "Loại hình", "Loai hinh", "Danh mục", "Danh muc", "Type", "businessType");
            int mapsIdx = getColumnIndex(headers, "Google Maps", "Google Map", "Maps", "googleMapUrl");
            int noteIdx = getColumnIndex(headers, "Ghi chú", "Ghi chu", "Note", "note");

            boolean hasHeader = true;
            if (nameIdx == -1 || phoneIdx == -1 || addrIdx == -1 || areaIdx == -1) {
                if (headers.size() >= 4) {
                    hasHeader = false;
                    nameIdx = 0;
                    addrIdx = 1;
                    areaIdx = 2;
                    phoneIdx = 3;
                    webIdx = headers.size() > 4 ? 4 : -1;
                    typeIdx = headers.size() > 5 ? 5 : -1;
                    mapsIdx = headers.size() > 6 ? 6 : -1;
                    noteIdx = headers.size() > 7 ? 7 : -1;
                } else {
                    return ResponseEntity.badRequest().body(Map.of("message", "File Excel không khớp các cột bắt buộc (Tên doanh nghiệp, Địa chỉ, Khu vực, Số điện thoại). Hãy tải file mẫu để kiểm tra."));
                }
            }

            int totalRows = 0;
            int successCount = 0;
            int failedCount = 0;
            List<Map<String, Object>> errors = new ArrayList<>();

            int startRow = hasHeader ? 1 : 0;
            for (int rNum = startRow; rNum <= sheet.getLastRowNum(); rNum++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(rNum);
                if (row == null) continue;

                boolean isEmpty = true;
                List<String> columns = new ArrayList<>();
                for (int i = 0; i < Math.max(7, row.getLastCellNum()); i++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(i);
                    String val = getCellValueAsString(cell);
                    columns.add(val);
                    if (!val.trim().isEmpty()) {
                        isEmpty = false;
                    }
                }
                if (isEmpty) continue;

                totalRows++;

                int maxIdx = Math.max(Math.max(Math.max(nameIdx, phoneIdx), Math.max(addrIdx, areaIdx)), Math.max(Math.max(webIdx, typeIdx), Math.max(mapsIdx, noteIdx)));
                if (columns.size() <= maxIdx) {
                    failedCount++;
                    errors.add(Map.of("row", rNum + 1, "message", "Dòng không đủ số cột dữ liệu"));
                    continue;
                }

                String businessName = columns.get(nameIdx).trim();
                String address = columns.get(addrIdx).trim();
                String area = columns.get(areaIdx).trim();
                String phone = columns.get(phoneIdx).trim();
                String website = webIdx != -1 ? columns.get(webIdx).trim() : "";
                String businessType = typeIdx != -1 ? columns.get(typeIdx).trim() : "";
                String googleMapUrl = mapsIdx != -1 ? columns.get(mapsIdx).trim() : "";
                String note = noteIdx != -1 && columns.size() > noteIdx ? columns.get(noteIdx).trim() : "";

                if (businessName.isEmpty()) {
                    failedCount++;
                    errors.add(Map.of("row", rNum + 1, "message", "Thiếu tên doanh nghiệp"));
                    continue;
                }

                if (!phone.isEmpty() && dataRecordRepository.existsByPhone(phone)) {
                    failedCount++;
                    errors.add(Map.of("row", rNum + 1, "message", "Số điện thoại đã tồn tại"));
                    continue;
                }

                try {
                    DataRecord record = new DataRecord();
                    record.setBusinessName(businessName);
                    record.setAddress(address);
                    record.setArea(area);
                    record.setPhone(phone);
                    record.setWebsite(website);
                    record.setBusinessType(businessType);
                    record.setGoogleMapUrl(googleMapUrl);
                    record.setNote(note);
                    record.setStatus("Chưa xử lý");
                    record.setAssignedTo(null);
                    record.setAssignedToName(null);
                    record.setCreatedAt(Instant.now());
                    record.setUpdatedAt(Instant.now());

                    dataRecordRepository.save(record);
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    errors.add(Map.of("row", rNum + 1, "message", "Lỗi lưu database: " + e.getMessage()));
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("totalRows", totalRows);
            response.put("successCount", successCount);
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
}
