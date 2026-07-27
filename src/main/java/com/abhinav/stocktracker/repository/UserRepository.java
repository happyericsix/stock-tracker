package com.abhinav.stocktracker.repository;

import com.abhinav.stocktracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByQqNumber(String qqNumber);
    boolean existsByQqNumber(String qqNumber);
    List<User> findByQqNumberIsNotNull();
}
