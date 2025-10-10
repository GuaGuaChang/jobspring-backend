package com.jobspring.jobspringbackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.jobspring.jobspringbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Query("""
                SELECT u FROM User u
                WHERE (:email IS NULL OR u.email LIKE %:email%)
                AND (:fullName IS NULL OR u.fullName LIKE %:fullName%)
                AND (:phone IS NULL OR u.phone LIKE %:phone%)
                AND (:id IS NULL OR u.id = :id)
            """)
    Page<User> searchUsers(@Param("email") String email, @Param("fullName") String fullName, @Param("phone") String phone, @Param("id") Long id, Pageable pageable);
}
