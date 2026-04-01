package io.temporal.demos.novabank.worker.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.demos.novabank.worker.model.KycRequest;
import io.temporal.demos.novabank.worker.model.KycResult;

@ActivityInterface
public interface KycActivities {
    KycResult verifyIdentityDocument(KycRequest request);
}
