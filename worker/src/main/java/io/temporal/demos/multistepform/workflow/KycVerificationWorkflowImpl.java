package io.temporal.demos.multistepform.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.demos.multistepform.activity.KycActivities;
import io.temporal.demos.multistepform.model.KycRequest;
import io.temporal.demos.multistepform.model.KycResult;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;

import java.time.Duration;

@WorkflowImpl(taskQueues = "multistep-form-demo")
public class KycVerificationWorkflowImpl implements KycVerificationWorkflow {
    private final KycActivities activities = Workflow.newActivityStub(
            KycActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .build())
                    .build());

    @Override
    public KycResult run(KycRequest request) {
        return activities.verifyIdentityDocument(request);
    }
}
