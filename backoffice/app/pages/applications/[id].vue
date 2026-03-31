<script setup lang="ts">
const route = useRoute()
const applicationId = route.params.id as string

const { data: state, refresh, status: fetchStatus } = useLazyFetch(`/api/applications/${applicationId}/state`)
const initialLoadDone = ref(false)
watch(fetchStatus, (s) => { if (s === 'success') initialLoadDone.value = true }, { immediate: true })

const formState = computed(() => state.value as any)
const formStatus = computed(() => formState.value?.status)
const isPendingReview = computed(() => formStatus.value === 'PendingReview')
const reviewDecision = computed(() => formState.value?.reviewDecision)
const kycInfo = computed(() => formState.value?.kyc)
const page1 = computed(() => formState.value?.page1)
const page2 = computed(() => formState.value?.page2)
const page3 = computed(() => formState.value?.page3)

const reason = ref('')
const submitting = ref(false)
const error = ref('')

// Poll while pending review
let pollInterval: ReturnType<typeof setInterval> | null = null

watch(isPendingReview, (pending) => {
  if (pending && !pollInterval) {
    pollInterval = setInterval(() => refresh(), 3000)
  } else if (!pending && pollInterval) {
    clearInterval(pollInterval)
    pollInterval = null
  }
}, { immediate: true })

onUnmounted(() => {
  if (pollInterval) clearInterval(pollInterval)
})

async function submitDecision(outcome: 'Approved' | 'Rejected') {
  error.value = ''
  submitting.value = true
  try {
    await $fetch(`/api/applications/${applicationId}/review`, {
      method: 'POST',
      body: { outcome, reason: reason.value },
    })
    await refresh()
  } catch (e: any) {
    error.value = e.data?.message || 'Failed to submit review decision.'
  } finally {
    submitting.value = false
  }
}

function statusClass(status: string) {
  const map: Record<string, string> = {
    PendingReview: 'pending-review',
    InProgress: 'in-progress',
    Approved: 'approved',
    Rejected: 'rejected',
    Abandoned: 'abandoned',
  }
  return map[status] || 'in-progress'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PendingReview: 'Pending Review',
    InProgress: 'In Progress',
    Approved: 'Approved',
    Rejected: 'Rejected',
    Abandoned: 'Abandoned',
  }
  return map[status] || status
}

function humanize(value: string) {
  if (!value) return ''
  return value.replace(/([a-z])([A-Z])/g, '$1 $2')
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('en-EU', { style: 'currency', currency: 'EUR' }).format(value)
}
</script>

<template>
  <main class="container" style="padding-top: 40px; padding-bottom: 64px;">
    <div v-if="!initialLoadDone" class="page-loading">
      <span class="spinner" />
      <span>Loading application...</span>
    </div>

    <template v-else-if="formState">
      <div class="detail-header">
        <div>
          <NuxtLink to="/" class="btn btn-secondary btn-sm" style="margin-bottom: 16px;">
            &larr; Back to Dashboard
          </NuxtLink>
          <h1>Application {{ applicationId }}</h1>
        </div>
        <div class="meta">
          <span v-if="kycInfo" class="kyc-badge" :class="kycInfo.status === 'Pending' ? 'pending' : kycInfo.status === 'Approved' ? 'approved' : 'failed'">
            KYC: {{ kycInfo.status }}
          </span>
          <span class="status-badge" :class="statusClass(formStatus)">
            {{ statusLabel(formStatus) }}
          </span>
        </div>
      </div>

      <!-- Decision Banner (if already reviewed) -->
      <div v-if="reviewDecision" class="decision-banner" :class="reviewDecision.outcome === 'Approved' ? 'approved' : 'rejected'">
        <span>{{ reviewDecision.outcome === 'Approved' ? '&#10003; Application Approved' : '&#10007; Application Rejected' }}</span>
        <span v-if="reviewDecision.reason" class="decision-reason">{{ reviewDecision.reason }}</span>
      </div>

      <!-- KYC Failed Warning -->
      <div v-if="kycInfo && kycInfo.status === 'Failed'" class="error-message">
        KYC verification failed{{ kycInfo.reason ? `: ${kycInfo.reason}` : '' }}
      </div>

      <div v-if="error" class="error-message">{{ error }}</div>

      <!-- Applicant Details -->
      <div v-if="page1" class="detail-card">
        <h2>Personal Identity</h2>
        <div class="summary-grid">
          <div class="summary-row">
            <span class="label">First Name</span>
            <span class="value">{{ page1.firstName }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Last Name</span>
            <span class="value">{{ page1.lastName }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Date of Birth</span>
            <span class="value">{{ page1.dateOfBirth }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Nationality</span>
            <span class="value">{{ page1.nationality }}</span>
          </div>
          <div class="summary-row">
            <span class="label">ID Type</span>
            <span class="value">{{ humanize(page1.idType) }}</span>
          </div>
          <div class="summary-row">
            <span class="label">ID Number</span>
            <span class="value">{{ page1.idNumber }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Email</span>
            <span class="value">{{ page1.email }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Phone</span>
            <span class="value">{{ page1.phone }}</span>
          </div>
        </div>
      </div>

      <div v-if="page2" class="detail-card">
        <h2>Residential Address</h2>
        <div class="summary-grid">
          <div class="summary-row full-width" style="grid-column: 1 / -1;">
            <span class="label">Street</span>
            <span class="value">{{ page2.street }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Postal Code</span>
            <span class="value">{{ page2.postalCode }}</span>
          </div>
          <div class="summary-row">
            <span class="label">City</span>
            <span class="value">{{ page2.city }}</span>
          </div>
          <div v-if="page2.stateProvince" class="summary-row">
            <span class="label">State / Province</span>
            <span class="value">{{ page2.stateProvince }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Country</span>
            <span class="value">{{ page2.country }}</span>
          </div>
        </div>
      </div>

      <div v-if="page3" class="detail-card">
        <h2>Financial Situation</h2>
        <div class="summary-grid">
          <div class="summary-row">
            <span class="label">Employment</span>
            <span class="value">{{ humanize(page3.employmentType) }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Employer</span>
            <span class="value">{{ page3.employer }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Monthly Income</span>
            <span class="value">{{ formatCurrency(page3.monthlyIncome) }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Monthly Expenses</span>
            <span class="value">{{ formatCurrency(page3.monthlyExpenses) }}</span>
          </div>
        </div>
      </div>

      <!-- No data yet -->
      <div v-if="!page1 && !page2 && !page3" class="detail-card">
        <p style="color: var(--gray); text-align: center; padding: 24px 0;">
          The applicant has not submitted any data yet.
        </p>
      </div>

      <!-- Review Actions (only for PendingReview) -->
      <div v-if="isPendingReview" class="review-actions">
        <h2>Review Decision</h2>
        <label style="font-size: 0.8125rem; font-weight: 600; display: block; margin-bottom: 8px;">
          Reason (optional)
        </label>
        <textarea v-model="reason" placeholder="Add a note about your decision..." />
        <div class="actions">
          <button class="btn btn-approve" :disabled="submitting" @click="submitDecision('Approved')">
            <span v-if="submitting" class="spinner" />
            Approve
          </button>
          <button class="btn btn-reject" :disabled="submitting" @click="submitDecision('Rejected')">
            <span v-if="submitting" class="spinner" />
            Reject
          </button>
        </div>
      </div>
    </template>

    <div v-else class="page-loading">
      <p>Application not found or workflow has completed.</p>
      <NuxtLink to="/" class="btn btn-secondary" style="margin-top: 16px;">&larr; Back to Dashboard</NuxtLink>
    </div>
  </main>
</template>
