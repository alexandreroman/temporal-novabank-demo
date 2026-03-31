package io.temporal.demos.multistepform.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.demos.multistepform.model.KycRequest;
import io.temporal.demos.multistepform.model.KycResult;

@ActivityInterface
public interface KycActivities {
    KycResult verifyIdentityDocument(KycRequest request);
}
