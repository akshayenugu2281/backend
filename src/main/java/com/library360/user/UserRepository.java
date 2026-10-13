package com.library360.user;

import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> lockByEmail(@Param("email") String email);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByAdmissionNumberIgnoreCase(String admissionNumber);
    boolean existsByRollNumberIgnoreCase(String rollNumber);
    boolean existsByFacultyIdIgnoreCase(String facultyId);
    boolean existsByEmployeeNumberIgnoreCase(String employeeNumber);
    boolean existsByAdmissionNumberIgnoreCaseAndIdNot(String admissionNumber, Long id);
    boolean existsByRollNumberIgnoreCaseAndIdNot(String rollNumber, Long id);
    boolean existsByFacultyIdIgnoreCaseAndIdNot(String facultyId, Long id);
    boolean existsByEmployeeNumberIgnoreCaseAndIdNot(String employeeNumber, Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findByIdWithRoles(Long id);

    @Query(value = "SELECT u FROM User u",
           countQuery = "SELECT COUNT(u) FROM User u")
    Page<User> findAllUsers(Pageable pageable);

    @Query(value = "SELECT DISTINCT u FROM User u LEFT JOIN u.roles r WHERE " +
           "(:q IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(u.admissionNumber, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(u.rollNumber, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(u.facultyId, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(u.employeeNumber, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(u.department, '')) LIKE LOWER(CONCAT('%', :q, '%'))) AND " +
           "(:role IS NULL OR r.name = :role)",
           countQuery = "SELECT COUNT(DISTINCT u.id) FROM User u LEFT JOIN u.roles r WHERE " +
           "(:q IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(u.admissionNumber, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(u.rollNumber, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(u.facultyId, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(u.employeeNumber, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(COALESCE(u.department, '')) LIKE LOWER(CONCAT('%', :q, '%'))) AND " +
           "(:role IS NULL OR r.name = :role)")
    Page<User> searchUsers(@Param("q") String q, @Param("role") String role, Pageable pageable);
}
