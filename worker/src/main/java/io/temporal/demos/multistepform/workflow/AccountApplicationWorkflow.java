package io.temporal.demos.multistepform.workflow;

import io.temporal.demos.multistepform.model.*;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface AccountApplicationWorkflow {
    @WorkflowMethod
    ApplicationResult run(String applicationId);

    @SignalMethod
    void submitPage1(PersonalInfoData data);

    @SignalMethod
    void submitPage2(AddressData data);

    @SignalMethod
    void submitPage3(FinancialData data);

    @SignalMethod
    void submitFinalForm();

    @SignalMethod
    void submitReviewDecision(ReviewDecision decision);

    @SignalMethod
    void goToPage(int page);

    @QueryMethod
    FormState getFormState();
}
