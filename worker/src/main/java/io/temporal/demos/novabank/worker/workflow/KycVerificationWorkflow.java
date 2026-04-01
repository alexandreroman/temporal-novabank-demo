package io.temporal.demos.novabank.worker.workflow;

import io.temporal.demos.novabank.worker.model.KycRequest;
import io.temporal.demos.novabank.worker.model.KycResult;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface KycVerificationWorkflow {
    @WorkflowMethod
    KycResult run(KycRequest request);
}
