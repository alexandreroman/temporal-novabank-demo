package io.temporal.demos.novabank.worker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.EnumNaming;
import com.fasterxml.jackson.databind.EnumNamingStrategies;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewDecision(
    Outcome outcome,
    String reason
) {

    @EnumNaming(EnumNamingStrategies.UpperCamelCaseStrategy.class)
    public enum Outcome {
        APPROVED,
        REJECTED
    }
}
