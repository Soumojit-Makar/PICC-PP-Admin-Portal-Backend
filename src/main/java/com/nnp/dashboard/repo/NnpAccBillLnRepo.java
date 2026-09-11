package com.nnp.dashboard.repo;

import com.nnp.dashboard.model.NnpAccBillLn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NnpAccBillLnRepo extends JpaRepository<NnpAccBillLn, String> {
    List<NnpAccBillLn> findByAccBill_AccBillId(String accBillId);
    boolean existsByAccBill_NnpAccount_AccIdAndAccBillLnDtBetween(String accId, LocalDateTime start, LocalDateTime end);
    List<NnpAccBillLn> findByAccBill_NnpAccount_AccIdAndAccBillLnDtBetween(String accId, LocalDateTime start, LocalDateTime end);
}
