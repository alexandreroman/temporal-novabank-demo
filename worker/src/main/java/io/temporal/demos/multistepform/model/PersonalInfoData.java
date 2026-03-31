package io.temporal.demos.multistepform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PersonalInfoData(
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    Nationality nationality,
    IdType idType,
    String idNumber,
    String email,
    String phone
) {}
