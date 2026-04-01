package io.temporal.demos.novabank.worker.activity;

import io.temporal.demos.novabank.worker.model.KycRequest;
import io.temporal.demos.novabank.worker.model.KycResult;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@ActivityImpl(taskQueues = "novabank")
public class KycActivitiesImpl implements KycActivities {
    private static final Logger LOGGER = LoggerFactory.getLogger(KycActivitiesImpl.class);
    private final Random random = new Random();

    @Override
    public KycResult verifyIdentityDocument(KycRequest request) {
        LOGGER.atInfo().addKeyValue("request", request).log("Verifying identity document");
        sleep(3000);

        final boolean error = random.nextInt(10) <= 5;
        if(error) {
            throw ApplicationFailure.newFailure("KYC verification system is not responding", "KycEndpointNotResponding");
        }

        LOGGER.atInfo().addKeyValue("request", request).log("Identity document verified successfully");
        return new KycResult(true, null);
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
