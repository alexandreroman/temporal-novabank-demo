package io.temporal.demos.multistepform.workflow;

import io.temporal.demos.multistepform.model.KycRequest;
import io.temporal.demos.multistepform.model.KycResult;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface KycVerificationWorkflow {
    @WorkflowMethod
    KycResult run(KycRequest request);
}
