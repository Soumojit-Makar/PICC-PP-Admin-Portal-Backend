package com.nnp.dashboard.repo;

import com.nnp.dashboard.model.TokenUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface TokenUsageRepo extends JpaRepository<TokenUsage, Long> {
    List<TokenUsage> findByAccId(String accId);
    List<TokenUsage> findByAccIdAndExecdttimeBetween(String accId, ZonedDateTime start, ZonedDateTime end);
}
