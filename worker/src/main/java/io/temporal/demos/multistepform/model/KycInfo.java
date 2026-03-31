package io.temporal.demos.multistepform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.EnumNamingStrategies;
import com.fasterxml.jackson.databind.EnumNamingStrategy;
import com.fasterxml.jackson.databind.annotation.EnumNaming;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record KycInfo(
    Status status,
    String workflowId,
    String reason
) {

    @EnumNaming(EnumNamingStrategies.UpperCamelCaseStrategy.class)
    public enum Status {
        PENDING,
        APPROVED,
        REVIEW_NEEDED,
        FAILED;

        private static final EnumNamingStrategy NAMING =
                EnumNamingStrategies.UPPER_CAMEL_CASE;

        public String label() {
            return NAMING.convertEnumToExternalName(name());
        }
    }

    public static KycInfo pending(String workflowId) {
        return new KycInfo(Status.PENDING, workflowId, null);
    }

    public static KycInfo approved(String workflowId) {
        return new KycInfo(Status.APPROVED, workflowId, null);
    }

    public static KycInfo failed(String workflowId, String reason) {
        return new KycInfo(Status.FAILED, workflowId, reason);
    }
}
