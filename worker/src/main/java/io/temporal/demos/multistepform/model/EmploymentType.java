package io.temporal.demos.multistepform.model;

import com.fasterxml.jackson.databind.annotation.EnumNaming;
import com.fasterxml.jackson.databind.EnumNamingStrategies;

@EnumNaming(EnumNamingStrategies.UpperCamelCaseStrategy.class)
public enum EmploymentType {
    EMPLOYED,
    SELF_EMPLOYED,
    FREELANCE,
    RETIRED,
    STUDENT,
    UNEMPLOYED
}
