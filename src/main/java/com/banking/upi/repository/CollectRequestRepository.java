package com.banking.upi.repository;

import com.banking.upi.domain.CollectRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollectRequestRepository extends JpaRepository<CollectRequest, Long> {
    Optional<CollectRequest> findByRequestRef(String requestRef);
    List<CollectRequest> findByFromVpaOrToVpa(String fromVpa, String toVpa);
}
