package io.temporal.demos.novabank.worker.model;

import com.fasterxml.jackson.databind.annotation.EnumNaming;
import com.fasterxml.jackson.databind.EnumNamingStrategies;

@EnumNaming(EnumNamingStrategies.UpperCamelCaseStrategy.class)
public enum Nationality {
    AMERICAN,
    SPANISH,
    FRENCH,
    BRITISH
}
