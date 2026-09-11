package com.nnp.dashboard.service;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.nnp.dashboard.model.EnvActivityLogV2;
import com.nnp.dashboard.repo.EnvActivityLogRepoV2;
import com.nnp.dashboard.repo.IDRepo;
import com.nnp.dashboard.vo.EnvActivityLogVOV2;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service that records and reads the environment activity log.
 *
 * Every step of the environment / user provisioning pipeline writes an activity
 * row (via {@link #logActivity}) with a description, note, status (start /
 * complete / failed) and ordering. This produces an audit trail used by the
 * operations UI and by {@code EnvActivityLogVOV2} consumers.
 */
@Service
@Slf4j
public class EnvActivityLogService {

    @Autowired
    private EnvActivityLogRepoV2 envActivityLogRepoV2;

    @Autowired
    private IDRepo idRepo;

    @Autowired
    private ModelMapper modelMapper;

    public void logActivity(EnvActivityLogVOV2 envActivityLogVOV2) {
        EnvActivityLogV2 envActivityLogV2 = new EnvActivityLogV2();
        envActivityLogV2.setActId("ACT_" + idRepo.getNextSeqVal());
        envActivityLogV2.setEnvId(envActivityLogVOV2.getEnvId());
        if (envActivityLogVOV2.getActDate() != null)
            envActivityLogV2.setActDate(envActivityLogVOV2.getActDate());
        else
            envActivityLogV2.setActDate(LocalDateTime.now());
        envActivityLogV2.setActDesc(envActivityLogVOV2.getActDesc());
        envActivityLogV2.setActStatus(envActivityLogVOV2.getActStatus());
        envActivityLogV2.setActNote(envActivityLogVOV2.getActNote());
        envActivityLogV2.setUserId(envActivityLogVOV2.getUserId());

        envActivityLogRepoV2.saveAndFlush(envActivityLogV2);
    }

    public List<EnvActivityLogVOV2> getActivityLogByEnvId(String envId) {
        return envActivityLogRepoV2.findByEnvIdOrderByActDateDesc(envId)
                .stream()
                .map(envActivityLogV2 -> modelMapper.map(envActivityLogV2, EnvActivityLogVOV2.class))
                .toList();
    }

    public Page<EnvActivityLogVOV2> getActivityLogByEnvIdWithPaging(String envId, int pageNumber, int size) {
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by("actDate").descending());
        Page<EnvActivityLogV2> envActivityLogV2Page = envActivityLogRepoV2.findByEnvId(envId, pageable);
        List<EnvActivityLogVOV2> envActivityLogVOV2List = envActivityLogV2Page.stream()
                .map(envActivityLogV2 -> modelMapper.map(envActivityLogV2, EnvActivityLogVOV2.class))
                .toList();
        return new PageImpl<>(envActivityLogVOV2List, envActivityLogV2Page.getPageable(),envActivityLogV2Page.getTotalElements());

    }

}
