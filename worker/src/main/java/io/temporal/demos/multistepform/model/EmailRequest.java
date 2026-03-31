package io.temporal.demos.multistepform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.EnumNaming;
import com.fasterxml.jackson.databind.EnumNamingStrategies;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record EmailRequest(
    String to,
    Template template,
    Map<String, String> context
) {

    @EnumNaming(EnumNamingStrategies.UpperCamelCaseStrategy.class)
    public enum Template {
        APPLICATION_UNDER_REVIEW,
        APPLICATION_APPROVED,
        APPLICATION_REJECTED,
        APPLICATION_ABANDONED
    }
}
