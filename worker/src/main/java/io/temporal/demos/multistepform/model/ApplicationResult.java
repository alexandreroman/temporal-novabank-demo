package io.temporal.demos.multistepform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApplicationResult(
        FormState.Status status,
        String firstName,
        String lastName,
        String email
) {
    public ApplicationResult(FormState.Status status) {
        this(status, null, null, null);
    }
}
