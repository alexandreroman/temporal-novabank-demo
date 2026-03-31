package io.temporal.demos.multistepform.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.common.SearchAttributeKey;
import io.temporal.demos.multistepform.activity.AccountApplicationActivities;
import io.temporal.demos.multistepform.model.*;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Async;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

@WorkflowImpl(taskQueues = "multistep-form-demo")
public class AccountApplicationWorkflowImpl implements AccountApplicationWorkflow {
    private static final Duration ABANDONMENT_TIMEOUT = Duration.ofMinutes(3);
    private static final SearchAttributeKey<String> REVIEW_STATUS =
            SearchAttributeKey.forKeyword("ReviewStatus");
    private static final SearchAttributeKey<String> KYC_STATUS =
            SearchAttributeKey.forKeyword("KycStatus");
    private static final SearchAttributeKey<String> APPLICANT_NAME =
            SearchAttributeKey.forText("ApplicantName");
    private final AccountApplicationActivities activities = Workflow.newActivityStub(
            AccountApplicationActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .build())
                    .build());

    private String applicationId;
    private PersonalInfoData page1Data;
    private AddressData page2Data;
    private FinancialData page3Data;
    private boolean finalSubmitted;
    private int pageOverride;
    private ReviewDecision reviewDecision;
    private FormState.Status formStatus = FormState.Status.IN_PROGRESS;
    private KycInfo.Status kycStatus = KycInfo.Status.PENDING;
    private String kycError;
    private int kycVersion;
    private boolean kycRestart;
    private Promise<KycResult> latestKycPromise;
    private long lastActivityTime;

    @Override
    public ApplicationResult run(String applicationId) {
        this.applicationId = applicationId;
        this.lastActivityTime = Workflow.currentTimeMillis();

        Workflow.upsertTypedSearchAttributes(
                REVIEW_STATUS.valueSet(formStatus.label()),
                KYC_STATUS.valueSet(kycStatus.label()));

        // Abandonment timer: resets on every form activity
        Async.procedure(() -> {
            while (formStatus == FormState.Status.IN_PROGRESS && !finalSubmitted) {
                long deadline = lastActivityTime + ABANDONMENT_TIMEOUT.toMillis();
                long remaining = deadline - Workflow.currentTimeMillis();
                if (remaining > 0) {
                    Workflow.sleep(Duration.ofMillis(remaining));
                }
                // If no new activity happened during sleep, abandon
                if (Workflow.currentTimeMillis() - lastActivityTime >= ABANDONMENT_TIMEOUT.toMillis()
                        && formStatus == FormState.Status.IN_PROGRESS && !finalSubmitted) {
                    formStatus = FormState.Status.ABANDONED;
                    Workflow.upsertTypedSearchAttributes(REVIEW_STATUS.valueSet(formStatus.label()));
                    if (page1Data != null) {
                        activities.sendEmail(new EmailRequest(
                                page1Data.email(),
                                EmailRequest.Template.APPLICATION_ABANDONED,
                                buildContext()));
                    }
                    return;
                }
            }
        });

        // Wait for page 1
        Workflow.await(() -> page1Data != null || formStatus == FormState.Status.ABANDONED);
        throwIfAbandoned();

        // Start KYC verification as child workflow
        startKycVerification();

        // Watch for identity changes and re-trigger KYC
        Async.procedure(() -> {
            while (formStatus == FormState.Status.IN_PROGRESS) {
                Workflow.await(() -> kycRestart || formStatus != FormState.Status.IN_PROGRESS);
                if (kycRestart) {
                    kycRestart = false;
                    startKycVerification();
                }
            }
        });

        Workflow.upsertTypedSearchAttributes(
                APPLICANT_NAME.valueSet(applicantFullName()));

        // Wait for page 2
        Workflow.await(() -> page2Data != null || formStatus == FormState.Status.ABANDONED);
        throwIfAbandoned();

        // Wait for page 3
        Workflow.await(() -> page3Data != null || formStatus == FormState.Status.ABANDONED);
        throwIfAbandoned();

        // Wait for final submission
        Workflow.await(() -> finalSubmitted || formStatus == FormState.Status.ABANDONED);
        throwIfAbandoned();

        // Ensure KYC is resolved before proceeding
        latestKycPromise.get();

        // Transition to PendingReview and notify reviewer
        formStatus = FormState.Status.PENDING_REVIEW;
        Workflow.upsertTypedSearchAttributes(REVIEW_STATUS.valueSet(formStatus.label()));
        activities.notifyReviewer(applicationId);

        // Wait for human review decision
        Workflow.await(() -> reviewDecision != null || formStatus == FormState.Status.ABANDONED);
        throwIfAbandoned();

        // Process decision
        if (reviewDecision.outcome() == ReviewDecision.Outcome.APPROVED) {
            formStatus = FormState.Status.APPROVED;
            Workflow.upsertTypedSearchAttributes(REVIEW_STATUS.valueSet(formStatus.label()));
            activities.createBankAccount(applicationId);
            activities.sendEmail(new EmailRequest(
                    page1Data.email(),
                    EmailRequest.Template.APPLICATION_APPROVED,
                    buildContext()));
        } else {
            formStatus = FormState.Status.REJECTED;
            Workflow.upsertTypedSearchAttributes(REVIEW_STATUS.valueSet(formStatus.label()));
            activities.sendEmail(new EmailRequest(
                    page1Data.email(),
                    EmailRequest.Template.APPLICATION_REJECTED,
                    buildContext()));
        }

        return new ApplicationResult(formStatus, page1Data.firstName(), page1Data.lastName(), page1Data.email());
    }

    @Override
    public void submitPage1(PersonalInfoData data) {
        this.lastActivityTime = Workflow.currentTimeMillis();
        if (page1Data != null && identityChanged(page1Data, data)) {
            kycRestart = true;
        }
        this.page1Data = data;
        this.pageOverride = 2;
    }

    @Override
    public void submitPage2(AddressData data) {
        this.lastActivityTime = Workflow.currentTimeMillis();
        this.page2Data = data;
        this.pageOverride = 3;
    }

    @Override
    public void submitPage3(FinancialData data) {
        this.lastActivityTime = Workflow.currentTimeMillis();
        this.page3Data = data;
        this.pageOverride = 4;
    }

    @Override
    public void submitFinalForm() {
        this.lastActivityTime = Workflow.currentTimeMillis();
        this.finalSubmitted = true;
        this.pageOverride = 0;
    }

    @Override
    public void submitReviewDecision(ReviewDecision decision) {
        this.reviewDecision = decision;
    }

    @Override
    public void goToPage(int page) {
        this.lastActivityTime = Workflow.currentTimeMillis();
        this.pageOverride = page;
    }

    private void startKycVerification() {
        final var version = ++kycVersion;
        kycStatus = KycInfo.Status.PENDING;
        kycError = null;
        Workflow.upsertTypedSearchAttributes(KYC_STATUS.valueSet(kycStatus.label()));

        final var kycWorkflowId = "kyc-verification-" + applicationId + "-" + version;
        final var kycWorkflow = Workflow.newChildWorkflowStub(
                KycVerificationWorkflow.class,
                ChildWorkflowOptions.newBuilder()
                        .setWorkflowId(kycWorkflowId)
                        .build());
        final var kycRequest = new KycRequest(
                page1Data.firstName(),
                page1Data.lastName(),
                page1Data.idType(),
                page1Data.idNumber(),
                page1Data.dateOfBirth());
        latestKycPromise = Async.function(kycWorkflow::run, kycRequest);

        latestKycPromise.thenApply(kycResult -> {
            if (version != kycVersion) return null;
            if (kycResult.approved()) {
                kycStatus = KycInfo.Status.APPROVED;
            } else {
                kycStatus = KycInfo.Status.FAILED;
                kycError = kycResult.reason();
            }
            Workflow.upsertTypedSearchAttributes(KYC_STATUS.valueSet(kycStatus.label()));
            return null;
        });
    }

    private void throwIfAbandoned() {
        if (formStatus == FormState.Status.ABANDONED) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "Application abandoned due to inactivity",
                    FormState.Status.ABANDONED.label(),
                    new ApplicationResult(formStatus));
        }
    }

    private boolean identityChanged(PersonalInfoData prev, PersonalInfoData next) {
        return !Objects.equals(prev.idType(), next.idType())
                || !Objects.equals(prev.idNumber(), next.idNumber())
                || !Objects.equals(prev.nationality(), next.nationality());
    }

    @Override
    public FormState getFormState() {
        final var kycWorkflowId = applicationId != null ? "kyc-verification-" + applicationId + "-" + kycVersion : null;
        final var kycInfo = switch (kycStatus) {
            case PENDING -> KycInfo.pending(kycWorkflowId);
            case APPROVED -> KycInfo.approved(kycWorkflowId);
            default -> KycInfo.failed(kycWorkflowId, kycError);
        };

        return new FormState(
                currentPage(),
                formStatus,
                page1Data,
                page2Data,
                page3Data,
                finalSubmitted,
                kycInfo,
                reviewDecision);
    }

    private int currentPage() {
        if (pageOverride > 0) return pageOverride;
        if (finalSubmitted) return 4;
        if (page3Data != null) return 4;
        if (page2Data != null) return 3;
        if (page1Data != null) return 2;
        return 1;
    }

    private String applicantFullName() {
        return page1Data != null
                ? page1Data.firstName() + " " + page1Data.lastName()
                : "Applicant";
    }

    private Map<String, String> buildContext() {
        return Map.of(
                "applicantName", applicantFullName(),
                "applicationId", applicationId);
    }
}
