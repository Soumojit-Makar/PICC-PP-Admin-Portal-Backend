package com.nnp.dashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Read-only JPA entity mapping to portal.token_usage for daily token cost aggregation.
 */
@Entity
@Table(name = "token_usage", schema = "portal")
@Immutable
@Getter
@Setter
public class TokenUsage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "acc_id")
    private String accId;

    @Column(name = "jobtask_id")
    private String jobtaskId;

    @Column(name = "job_type")
    private String jobType;

    @Column(name = "model")
    private String model;

    @Column(name = "input_token")
    private Long inputToken;

    @Column(name = "output_token")
    private Long outputToken;

    @Column(name = "input_cost", precision = 10, scale = 6)
    private BigDecimal inputCost;

    @Column(name = "output_cost", precision = 10, scale = 6)
    private BigDecimal outputCost;

    @Column(name = "execdttime")
    private ZonedDateTime execdttime;
}
