package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nnp.dashboard.model.NnpAccComm;

import java.util.List;

public interface NnpAccCommunicationRepo extends JpaRepository<NnpAccComm, String> {
    List<NnpAccComm> findByNnpAccount_AccName(String accName);
    List<NnpAccComm> findByNnpAccount_AccIdOrderByCommDateDesc(String accId);
}
