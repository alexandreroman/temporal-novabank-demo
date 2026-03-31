package io.temporal.demos.multistepform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FinancialData(
    EmploymentType employmentType,
    String employer,
    BigDecimal monthlyIncome,
    BigDecimal monthlyExpenses
) {}
