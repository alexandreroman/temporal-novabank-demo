package io.temporal.demos.novabank.worker;

import io.temporal.demos.novabank.worker.activity.AccountApplicationActivities;
import io.temporal.demos.novabank.worker.activity.AccountApplicationActivitiesImpl;
import io.temporal.demos.novabank.worker.activity.KycActivities;
import io.temporal.demos.novabank.worker.activity.KycActivitiesImpl;
import io.temporal.demos.novabank.worker.workflow.AccountApplicationWorkflow;
import io.temporal.demos.novabank.worker.workflow.AccountApplicationWorkflowImpl;
import io.temporal.demos.novabank.worker.workflow.KycVerificationWorkflow;
import io.temporal.demos.novabank.worker.workflow.KycVerificationWorkflowImpl;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * Registers proxy and reflection hints that Spring AOT cannot infer for this Temporal application.
 *
 * <p>This covers the JDK dynamic proxies Temporal builds for workflow and activity stubs at runtime,
 * the Jackson enum-naming strategy instantiated reflectively, and the workflow/activity types
 * Temporal instantiates and invokes reflectively. The Jackson payload models are registered via
 * {@code @RegisterReflectionForBinding} on {@link NativeConfig}.
 */
class NativeRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // These JDK dynamic proxy interface combinations were captured from the Temporal SDK at
        // runtime via the GraalVM native-image tracing agent. Activity and child workflow stubs are
        // composite proxies that also implement internal SDK marker interfaces, which is why the
        // marker types are referenced by name rather than imported.
        hints.proxies()
                .registerJdkProxy(
                        TypeReference.of(
                                "io.temporal.demos.novabank.worker.activity.AccountApplicationActivities"),
                        TypeReference.of("io.temporal.internal.sync.AsyncInternal$AsyncMarker"));
        hints.proxies()
                .registerJdkProxy(
                        TypeReference.of(
                                "io.temporal.demos.novabank.worker.activity.KycActivities"),
                        TypeReference.of("io.temporal.internal.sync.AsyncInternal$AsyncMarker"));
        hints.proxies()
                .registerJdkProxy(
                        TypeReference.of(
                                "io.temporal.demos.novabank.worker.workflow.KycVerificationWorkflow"),
                        TypeReference.of("io.temporal.internal.sync.StubMarker"),
                        TypeReference.of("io.temporal.internal.sync.AsyncInternal$AsyncMarker"));
        hints.proxies()
                .registerJdkProxy(
                        TypeReference.of("io.temporal.client.schedules.ScheduleClient"));

        // The workflow/activity types Temporal instantiates and invokes reflectively at runtime.
        registerForReflection(
                hints,
                AccountApplicationWorkflowImpl.class,
                KycVerificationWorkflowImpl.class,
                AccountApplicationWorkflow.class,
                KycVerificationWorkflow.class,
                AccountApplicationActivities.class,
                KycActivities.class,
                AccountApplicationActivitiesImpl.class,
                KycActivitiesImpl.class);
    }

    private static void registerForReflection(RuntimeHints hints, Class<?>... types) {
        for (var type : types) {
            hints.reflection()
                    .registerType(
                            type,
                            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                            MemberCategory.INVOKE_DECLARED_METHODS);
        }
    }
}
