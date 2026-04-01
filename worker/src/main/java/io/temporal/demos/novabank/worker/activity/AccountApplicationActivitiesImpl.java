package io.temporal.demos.novabank.worker.activity;

import io.temporal.demos.novabank.worker.model.EmailRequest;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(taskQueues = "novabank")
public class AccountApplicationActivitiesImpl implements AccountApplicationActivities {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountApplicationActivitiesImpl.class);

    @Override
    public void createBankAccount(String applicationId) {
        LOGGER.atInfo().addKeyValue("applicationId", applicationId).log("Creating bank account");
        sleep(2000);
        LOGGER.atInfo().addKeyValue("applicationId", applicationId).log("Bank account created");
    }

    @Override
    public void sendEmail(EmailRequest request) {
        LOGGER.atInfo().addKeyValue("template", request.template()).addKeyValue("to", request.to()).log("Sending email");
        sleep(500);
        LOGGER.atInfo().addKeyValue("template", request.template()).addKeyValue("to", request.to()).log("Email sent");
    }

    @Override
    public void notifyReviewer(String applicationId) {
        LOGGER.atInfo().addKeyValue("applicationId", applicationId).log("Notifying reviewer");
        sleep(500);
        LOGGER.atInfo().addKeyValue("applicationId", applicationId).log("Reviewer notified");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
