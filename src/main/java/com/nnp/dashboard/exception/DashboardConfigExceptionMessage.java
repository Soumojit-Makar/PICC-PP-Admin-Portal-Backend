package com.nnp.dashboard.exception;

import lombok.*;

/**
 * Payload object describing an error: an HTTP status {@code code} and a
 * {@code message}. Used both for throwing and for returning errors to the
 * front-end in a standard format.
 *
 * @author AC
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardConfigExceptionMessage {
	private String code;
	private String message;
}
