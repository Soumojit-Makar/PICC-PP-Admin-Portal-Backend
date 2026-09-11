package com.nnp.dashboard.vo.mail;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailAttachmentVO {
	
	private Long attachmentId ;
	
	private String name ;
	
	private String contentType ;
	
	private MultipartFile file;
}
