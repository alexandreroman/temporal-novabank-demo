package io.temporal.demos.multistepform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AddressData(
    String street,
    String postalCode,
    String city,
    String stateProvince,
    String country
) {}
