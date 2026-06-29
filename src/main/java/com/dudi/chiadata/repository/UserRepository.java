package com.dudi.chiadata.repository;

import com.dudi.chiadata.model.User;
import com.dudi.chiadata.model.Role;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByRoleAndFullNameContainingIgnoreCase(Role role, String fullName);
    List<User> findByRoleAndPhone(Role role, String phone);
}
