package io.temporal.demos.multistepform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.EnumNamingStrategies;
import com.fasterxml.jackson.databind.EnumNamingStrategy;
import com.fasterxml.jackson.databind.annotation.EnumNaming;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FormState(
    int currentPage,
    Status status,
    PersonalInfoData page1,
    AddressData page2,
    FinancialData page3,
    boolean finalSubmitted,
    KycInfo kyc,
    ReviewDecision reviewDecision
) {

    @EnumNaming(EnumNamingStrategies.UpperCamelCaseStrategy.class)
    public enum Status {
        IN_PROGRESS,
        PENDING_REVIEW,
        APPROVED,
        REJECTED,
        ABANDONED;

        private static final EnumNamingStrategy NAMING =
                EnumNamingStrategies.UPPER_CAMEL_CASE;

        public String label() {
            return NAMING.convertEnumToExternalName(name());
        }
    }
}
