package com.nnp.dashboard.service;

import java.io.File;
import java.util.concurrent.ExecutionException;

import com.nnp.dashboard.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nnp.dashboard.exception.DashboardConfigException;
import com.nnp.dashboard.exception.DashboardConfigExceptionMessage;
import com.nnp.dashboard.vo.mail.EmailVO;

import lombok.extern.slf4j.Slf4j;

/**
 * Integration service for the Mail micro-service, using the
 * {@code webClientMailService} bean (see {@code DashboardConfiguratorConfig}).
 *
 * Provides simple text emails and multipart emails with a K8s-token file
 * attachment (used by the onboarding welcome emails).
 * Both methods run with {@code NOT_SUPPORTED} so a mail call never participates
 * in the caller's DB transaction (avoiding a remote call dragging on a
 * transaction / blocking a rollback).
 */
@Service
@Slf4j
public class MailService {

    @Qualifier("webClientMailService")
    @Autowired
    private WebClient webClientMailService;
    
    private ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public EmailVO sendUserWelcomeMail(EmailVO emailVO) {
        ResponseEntity<EmailVO> emailVOResponseEntity;
        try {
            emailVOResponseEntity = webClientMailService.post()
                    .uri("/api/v1/sendmail")
                    .body(BodyInserters.fromValue(emailVO))
                    .retrieve()
                    .toEntity(EmailVO.class)
                    .toFuture()
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            //throw new RuntimeException(e);
            log.error("Send Email call failed exception message --> {}", Utils.sanitizeForLog( e.getMessage()));
            throw new DashboardConfigException(new DashboardConfigExceptionMessage("500", "Send Email call failed"), e);
        }
        return emailVOResponseEntity.getBody();
    }
    
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
	public EmailVO sendMailWithAttachement(EmailVO emailVO,File attachement) {
		ResponseEntity<EmailVO> emailVOResponseEntity;
		
		try {
			objectMapper = new ObjectMapper();
			MultipartBodyBuilder builder = new MultipartBodyBuilder();
			builder.part("emailVoJson", objectMapper.writeValueAsString(emailVO));
			builder.part("document",new FileSystemResource(attachement));
					
			emailVOResponseEntity = webClientMailService.post()
					.uri("/api/v1/sendmailWithAttachment")
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(BodyInserters.fromMultipartData(builder.build()))
					.retrieve()
					.toEntity(EmailVO.class)
					.toFuture()
					.get();
					
		} catch (InterruptedException | ExecutionException | JsonProcessingException e) {

			log.error("Send Email call failed exception message --> {}", Utils.sanitizeForLog(e.getMessage()));
			throw new DashboardConfigException(new DashboardConfigExceptionMessage("500", "Send Email call failed"), e);
		}
		return emailVOResponseEntity.getBody();
	}
}
