package io.temporal.demos.novabank.worker.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.demos.novabank.worker.model.EmailRequest;

@ActivityInterface
public interface AccountApplicationActivities {
    void createBankAccount(String applicationId);

    void sendEmail(EmailRequest request);

    void notifyReviewer(String applicationId);
}
