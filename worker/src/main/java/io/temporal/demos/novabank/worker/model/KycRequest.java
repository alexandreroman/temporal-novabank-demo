package io.temporal.demos.novabank.worker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record KycRequest(
    String firstName,
    String lastName,
    IdType idType,
    String idNumber,
    LocalDate dateOfBirth
) {}
