package io.temporal.demos.novabank.worker;

import io.temporal.demos.novabank.worker.model.*;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(NativeRuntimeHints.class)
@RegisterReflectionForBinding({
        PersonalInfoData.class,
        AddressData.class,
        FinancialData.class,
        ApplicationResult.class,
        FormState.class,
        KycInfo.class,
        KycRequest.class,
        KycResult.class,
        ReviewDecision.class,
        EmailRequest.class
})
class NativeConfig {
}
