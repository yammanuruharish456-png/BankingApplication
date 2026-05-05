package com.banking.upi.repository;

import com.banking.upi.domain.Vpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VpaRepository extends JpaRepository<Vpa, Long> {
    Optional<Vpa> findByHandle(String handle);
    List<Vpa> findByUserId(Long userId);
    boolean existsByHandle(String handle);
}
