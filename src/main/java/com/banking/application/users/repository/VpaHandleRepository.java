package com.banking.application.users.repository;

import com.banking.application.users.entity.VpaHandle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VpaHandleRepository extends JpaRepository<VpaHandle, Long> {
    Optional<VpaHandle> findByVpa(String vpa);
    Optional<VpaHandle> findFirstByUserId(Long userId);
    List<VpaHandle> findAllByUserId(Long userId);
}
