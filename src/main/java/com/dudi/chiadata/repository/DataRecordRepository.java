package com.dudi.chiadata.repository;

import com.dudi.chiadata.model.DataRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface DataRecordRepository extends MongoRepository<DataRecord, String> {
    List<DataRecord> findByAssignedTo(String assignedTo);
    long countByAssignedTo(String assignedTo);
    long countByStatus(String status);
    long countByAssignedToAndStatus(String assignedTo, String status);
    Boolean existsByPhone(String phone);
}
