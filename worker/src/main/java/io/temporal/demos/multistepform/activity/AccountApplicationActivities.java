package io.temporal.demos.multistepform.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.demos.multistepform.model.EmailRequest;

@ActivityInterface
public interface AccountApplicationActivities {
    void createBankAccount(String applicationId);

    void sendEmail(EmailRequest request);

    void sendEscalationAlert(String applicationId);

    void notifyReviewer(String applicationId);
}
