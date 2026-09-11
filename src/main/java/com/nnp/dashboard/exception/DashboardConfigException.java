package com.nnp.dashboard.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * Standard runtime exception for the dashboard-configurator service.
 *
 * It carries a structured {@link DashboardConfigExceptionMessage} (HTTP code +
 * human readable message) so that the global handler
 * ({@link DashboardConfigExceptionHandler}) can translate it into a uniform
 * JSON error response.
 */
@Getter
@Setter
public class DashboardConfigException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4025085269928881747L;

	private DashboardConfigExceptionMessage dashboardConfigExceptionMessage;

    public DashboardConfigException(DashboardConfigExceptionMessage dashboardConfigExceptionMessage) {
		super(dashboardConfigExceptionMessage.getMessage());
		this.dashboardConfigExceptionMessage = dashboardConfigExceptionMessage;
	}

	public DashboardConfigException(DashboardConfigExceptionMessage dashboardConfigExceptionMessage, Exception ex) {
		super(dashboardConfigExceptionMessage.getMessage(), ex);
		this.dashboardConfigExceptionMessage = dashboardConfigExceptionMessage;
	}

}
