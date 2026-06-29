package com.dudi.chiadata.repository;

import com.dudi.chiadata.model.EmployeeProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface EmployeeProfileRepository extends MongoRepository<EmployeeProfile, String> {
    Optional<EmployeeProfile> findByEmployeeId(String employeeId);
}
