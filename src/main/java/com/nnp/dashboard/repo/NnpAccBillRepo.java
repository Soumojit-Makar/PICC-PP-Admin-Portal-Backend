package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nnp.dashboard.model.NnpAccBill;

import java.util.List;
import java.util.Optional;

public interface NnpAccBillRepo extends JpaRepository<NnpAccBill, String> {
    Optional<NnpAccBill> findFirstByNnpAccount_AccIdAndAccBillStatusOrderByAccBillDtDesc(String accId, String status);
    List<NnpAccBill> findByNnpAccount_AccIdOrderByAccBillDtDesc(String accId);
    List<NnpAccBill> findByNnpAccount_AccIdAndAccBillStatus(String accId, String status);

    /** Returns next value from the shared cn_hosting_generic_id_seq sequence. */
    @Query(value = "SELECT nextval('cn_hosting_generic_id_seq')", nativeQuery = true)
    Long nextSequenceValue();
}

