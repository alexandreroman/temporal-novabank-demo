<script setup lang="ts">
const route = useRoute()
const applicationId = route.params.id as string

const { data: formState, refresh, status: fetchStatus } = useLazyFetch(`/api/applications/${applicationId}/state`)

const submitting = ref(false)
const error = ref('')
const termsAccepted = ref(false)
const initialLoadDone = ref(false)
watch(fetchStatus, (s) => { if (s === 'success') initialLoadDone.value = true }, { immediate: true })

const currentPage = computed(() => (formState.value as any)?.currentPage ?? 1)
const _kycRaw = computed(() => (formState.value as any)?.kyc)
const kycStatus = ref<{ status: string; reason?: string } | undefined>()
watch(_kycRaw, (v) => { if (v) kycStatus.value = v }, { immediate: true })
const formStatus = computed(() => (formState.value as any)?.status)
const isInProgress = computed(() => formStatus.value === 'InProgress')
const reviewReason = computed(() => (formState.value as any)?.reviewDecision?.reason)

const applicantName = computed(() => {
  const p1 = (formState.value as any)?.page1
  return p1 ? `${p1.firstName} ${p1.lastName}` : 'Applicant'
})

const pageData = computed(() => ({
  page1: (formState.value as any)?.page1,
  page2: (formState.value as any)?.page2,
  page3: (formState.value as any)?.page3,
}))

const statusConfig = computed(() => {
  const configs: Record<string, { title: string; icon: string; class: string; label: string; message: string }> = {
    Approved: {
      title: 'Application Approved', icon: '&#10003;', class: 'status-approved', label: 'Approved',
      message: `Congratulations, ${applicantName.value}.\nYour account has been approved.`,
    },
    Rejected: {
      title: 'Application Declined', icon: '&#10007;', class: 'status-rejected', label: 'Rejected',
      message: `We're sorry, ${applicantName.value}.\nYour application could not be approved at this time.`,
    },
    Abandoned: {
      title: 'Application Expired', icon: '&#10007;', class: 'status-abandoned', label: 'Expired',
      message: `Your application has expired due to inactivity, ${applicantName.value}.`,
    },
  }
  return configs[formStatus.value] ?? {
    title: 'Application Submitted', icon: '&#9202;', class: 'status-pending', label: 'Under Review',
    message: `Thank you, ${applicantName.value}.\nYour application has been received and is now being reviewed by our compliance team.`,
  }
})

// Poll while PendingReview to detect approval/rejection
let statusPoll: ReturnType<typeof setInterval> | null = null

watch(formStatus, (s) => {
  if (s === 'PendingReview' && !statusPoll) {
    statusPoll = setInterval(() => refresh(), 2000)
  } else if (s !== 'PendingReview' && statusPoll) {
    clearInterval(statusPoll)
    statusPoll = null
  }
}, { immediate: true })

// Poll while InProgress to detect abandonment timeout
let abandonPoll: ReturnType<typeof setInterval> | null = null

watch(isInProgress, (active) => {
  if (active && !abandonPoll) {
    abandonPoll = setInterval(() => refresh(), 5000)
  } else if (!active && abandonPoll) {
    clearInterval(abandonPoll)
    abandonPoll = null
  }
}, { immediate: true })

// Poll while KYC is pending and badge is visible (pages 3-4)
let kycPoll: ReturnType<typeof setInterval> | null = null
const kycPending = computed(() => kycStatus.value?.status === 'Pending' && currentPage.value >= 2 && isInProgress.value)

watch(kycPending, (need) => {
  if (need && !kycPoll) {
    kycPoll = setInterval(() => refresh(), 2000)
  } else if (!need && kycPoll) {
    clearInterval(kycPoll)
    kycPoll = null
  }
}, { immediate: true })

onUnmounted(() => {
  if (statusPoll) clearInterval(statusPoll)
  if (abandonPoll) clearInterval(abandonPoll)
  if (kycPoll) clearInterval(kycPoll)
})

// ── Form data ──
const page1 = reactive({
  firstName: '', lastName: '', dateOfBirth: '', nationality: 'French',
  idType: 'Passport', idNumber: '', email: '', phone: '',
})

const page2 = reactive({
  street: '', postalCode: '', city: '', stateProvince: '', country: 'France',
})

const page3 = reactive({
  employmentType: 'Employed', employer: '', monthlyIncome: 0, monthlyExpenses: 0,
})

const pages = [page1, page2, page3] as const

// Pre-fill forms once when resuming a saved application
const formPrefilled = ref(false)

watch(formState, (state: any) => {
  if (!state || formPrefilled.value) return
  if (state.page1) Object.assign(page1, state.page1)
  if (state.page2) Object.assign(page2, state.page2)
  if (state.page3) Object.assign(page3, state.page3)
  formPrefilled.value = true
}, { immediate: true })

const stepLabels = ['Identity', 'Address', 'Financial', 'Summary']

async function submitPage(page: number) {
  error.value = ''
  submitting.value = true
  try {
    await $fetch(`/api/applications/${applicationId}/pages/${page}`, { method: 'POST', body: pages[page - 1] })
  } catch (e: any) {
    error.value = e.data?.message || 'An error occurred. Please try again.'
  } finally {
    await refresh()
    submitting.value = false
  }
}

async function goToPage(page: number) {
  error.value = ''
  submitting.value = true
  try {
    await $fetch(`/api/applications/${applicationId}/go-to-page`, { method: 'POST', body: { page } })
  } catch (e: any) {
    error.value = e.data?.message || 'An error occurred. Please try again.'
  } finally {
    await refresh()
    submitting.value = false
  }
}

async function submitFinal() {
  error.value = ''
  submitting.value = true
  try {
    await $fetch(`/api/applications/${applicationId}/submit`, { method: 'POST' })
    const poll = setInterval(async () => {
      await refresh()
      if (!isInProgress.value) {
        clearInterval(poll)
        submitting.value = false
      }
    }, 1000)
    onUnmounted(() => clearInterval(poll))
  } catch (e: any) {
    error.value = e.data?.message || 'An error occurred. Please try again.'
    submitting.value = false
  }
}

function humanize(value: string) {
  return value.replace(/([a-z])([A-Z])/g, '$1 $2')
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('en-EU', { style: 'currency', currency: 'EUR' }).format(value)
}

// ── Demo auto-fill ──
function pick<T>(arr: T[]): T {
  return arr[Math.floor(Math.random() * arr.length)]
}

function randomDigits(len: number) {
  return Array.from({ length: len }, () => Math.floor(Math.random() * 10)).join('')
}

function fillPage1() {
  const firstNames = ['Emma', 'Lucas', 'Chloé', 'Hugo', 'Alice', 'Louis', 'Léa', 'Gabriel', 'Manon', 'Raphaël']
  const lastNames = ['Martin', 'Bernard', 'Dubois', 'Laurent', 'Moreau', 'Simon', 'Michel', 'Garcia', 'Roux', 'Blanc']
  const year = 1970 + Math.floor(Math.random() * 25)
  const month = String(Math.floor(Math.random() * 12) + 1).padStart(2, '0')
  const day = String(Math.floor(Math.random() * 28) + 1).padStart(2, '0')
  const first = pick(firstNames)
  const last = pick(lastNames)

  Object.assign(page1, {
    firstName: first, lastName: last,
    dateOfBirth: `${year}-${month}-${day}`,
    nationality: pick(['French', 'Spanish', 'British', 'American']),
    idType: pick(['Passport', 'NationalId', 'DrivingLicense']),
    idNumber: randomDigits(9),
    email: `${first.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()}.${last.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()}@example.com`,
    phone: `+33 6 ${randomDigits(2)} ${randomDigits(2)} ${randomDigits(2)} ${randomDigits(2)}`,
  })
}

function fillPage2() {
  const streets = ['12 Rue de la Paix', '8 Avenue des Champs-Élysées', '27 Boulevard Haussmann', '3 Place Vendôme', '15 Rue du Faubourg Saint-Honoré']
  const cities = [
    { city: 'Paris', postalCode: '75001', stateProvince: 'Île-de-France' },
    { city: 'Lyon', postalCode: '69001', stateProvince: 'Auvergne-Rhône-Alpes' },
    { city: 'Marseille', postalCode: '13001', stateProvince: 'Provence-Alpes-Côte d\'Azur' },
    { city: 'Bordeaux', postalCode: '33000', stateProvince: 'Nouvelle-Aquitaine' },
    { city: 'Toulouse', postalCode: '31000', stateProvince: 'Occitanie' },
  ]
  const loc = pick(cities)
  Object.assign(page2, {
    street: pick(streets),
    postalCode: loc.postalCode, city: loc.city, stateProvince: loc.stateProvince,
    country: pick(['France', 'Spain', 'United Kingdom', 'United States']),
  })
}

function fillPage3() {
  const employers = ['Temporal Technologies', 'Airbus', 'BNP Paribas', 'Capgemini', 'Dassault Systèmes', 'TotalEnergies']
  const income = (Math.floor(Math.random() * 60) + 20) * 100
  Object.assign(page3, {
    employmentType: pick(['Employed', 'SelfEmployed', 'Freelance']),
    employer: pick(employers),
    monthlyIncome: income,
    monthlyExpenses: Math.round(income * (0.3 + Math.random() * 0.3) / 100) * 100,
  })
}
</script>

<template>
  <div>
    <header class="site-header">
      <div class="container">
        <NuxtLink to="/" class="logo">NovaBank</NuxtLink>
        <nav class="header-nav">
          <a href="https://temporal.io" target="_blank">Powered by <img src="~/assets/images/temporal-logo.png" alt="Temporal" class="temporal-logo"></a>
        </nav>
      </div>
    </header>

    <div v-if="!initialLoadDone" class="page-loading">
      <span class="spinner" />
      <span>Loading your application...</span>
    </div>

    <main v-else class="container" style="padding-top: 48px; padding-bottom: 64px;">
      <!-- ── Application Status (non-InProgress) ── -->
      <div v-if="!isInProgress" class="confirmation-card">
        <div class="confirmation-icon" :class="statusConfig.class" v-html="statusConfig.icon" />
        <h1>{{ statusConfig.title }}</h1>
        <p style="white-space: pre-line">{{ statusConfig.message }}</p>

        <p v-if="reviewReason && formStatus !== 'Approved'" class="review-reason">
          <strong>Reason:</strong> {{ reviewReason }}
        </p>

        <p v-if="formStatus === 'PendingReview'" class="hint">
          This page updates automatically. You will also receive an email.
        </p>
        <p v-else class="hint">
          An email has been sent to your registered address.
        </p>

        <div style="margin-top: 32px;">
          <NuxtLink to="/" class="btn btn-secondary">Back to Home</NuxtLink>
        </div>
      </div>

      <!-- ── Form (InProgress) ── -->
      <template v-else>
      <!-- Step Indicator -->
      <div class="steps">
        <template v-for="(label, i) in stepLabels" :key="i">
          <div
            class="step"
            :class="{ active: currentPage === i + 1, done: currentPage > i + 1, clickable: currentPage > i + 1 }"
            @click="currentPage > i + 1 ? goToPage(i + 1) : undefined"
          >
            <span class="step-number">
              <template v-if="currentPage > i + 1">&#10003;</template>
              <template v-else>{{ i + 1 }}</template>
            </span>
            <span class="step-label">{{ label }}</span>
          </div>
          <div
            v-if="i < stepLabels.length - 1"
            class="step-connector"
            :class="{ done: currentPage > i + 1 }"
          />
        </template>
      </div>

      <div v-if="error" class="error-message" style="max-width: 640px; margin: 0 auto 20px;">
        {{ error }}
      </div>

      <!-- ── Page 1: Personal Identity ── -->
      <div v-if="currentPage === 1" class="form-card">
        <h2>Personal Identity</h2>
        <p class="subtitle">Tell us about yourself to get started.</p>
        <button type="button" class="btn btn-demo" @click="fillPage1">Auto-fill demo data</button>
        <form @submit.prevent="submitPage(1)">
          <div class="form-grid">
            <div class="form-group">
              <label for="firstName">First Name</label>
              <input id="firstName" v-model="page1.firstName" required placeholder="Alexandre" />
            </div>
            <div class="form-group">
              <label for="lastName">Last Name</label>
              <input id="lastName" v-model="page1.lastName" required placeholder="Roman" />
            </div>
            <div class="form-group">
              <label for="dob">Date of Birth</label>
              <input id="dob" v-model="page1.dateOfBirth" type="date" required />
            </div>
            <div class="form-group">
              <label for="nationality">Nationality</label>
              <select id="nationality" v-model="page1.nationality" required>
                <option value="American">American</option>
                <option value="Spanish">Spanish</option>
                <option value="French">French</option>
                <option value="British">British</option>
              </select>
            </div>
            <div class="form-group">
              <label for="idType">ID Type</label>
              <select id="idType" v-model="page1.idType" required>
                <option value="Passport">Passport</option>
                <option value="NationalId">National ID Card</option>
                <option value="DrivingLicense">Driving License</option>
              </select>
            </div>
            <div class="form-group">
              <label for="idNumber">ID Number</label>
              <input id="idNumber" v-model="page1.idNumber" required placeholder="984156732" />
            </div>
            <div class="form-group">
              <label for="email">Email</label>
              <input id="email" v-model="page1.email" type="email" required placeholder="alex@example.com" />
            </div>
            <div class="form-group">
              <label for="phone">Phone</label>
              <input id="phone" v-model="page1.phone" type="tel" required placeholder="+33 6 12 34 56 78" />
            </div>
          </div>
          <div class="form-actions end">
            <button type="submit" class="btn btn-primary" :disabled="submitting">
              <span v-if="submitting" class="spinner" />
              {{ submitting ? 'Saving...' : 'Continue' }}
            </button>
          </div>
        </form>
      </div>

      <!-- ── Page 2: Residential Address ── -->
      <div v-if="currentPage === 2" class="form-card">
        <h2>Residential Address</h2>
        <p class="subtitle">Where do you currently live?</p>
        <KycBadge v-if="kycStatus" :kyc="kycStatus" />
        <button type="button" class="btn btn-demo" @click="fillPage2">Auto-fill demo data</button>
        <form @submit.prevent="submitPage(2)">
          <div class="form-grid">
            <div class="form-group full-width">
              <label for="street">Street Address</label>
              <input id="street" v-model="page2.street" required placeholder="42 Rue de Rivoli" />
            </div>
            <div class="form-group">
              <label for="postalCode">Postal Code</label>
              <input id="postalCode" v-model="page2.postalCode" required placeholder="75001" />
            </div>
            <div class="form-group">
              <label for="city">City</label>
              <input id="city" v-model="page2.city" required placeholder="Paris" />
            </div>
            <div class="form-group">
              <label for="stateProvince">State / Province</label>
              <input id="stateProvince" v-model="page2.stateProvince" placeholder="Île-de-France" />
            </div>
            <div class="form-group">
              <label for="country">Country</label>
              <select id="country" v-model="page2.country" required>
                <option value="France">France</option>
                <option value="Spain">Spain</option>
                <option value="United Kingdom">United Kingdom</option>
                <option value="United States">United States</option>
              </select>
            </div>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-secondary" :disabled="submitting" @click="goToPage(1)">
              Previous
            </button>
            <button type="submit" class="btn btn-primary" :disabled="submitting">
              <span v-if="submitting" class="spinner" />
              {{ submitting ? 'Saving...' : 'Continue' }}
            </button>
          </div>
        </form>
      </div>

      <!-- ── Page 3: Financial Situation ── -->
      <div v-if="currentPage === 3" class="form-card">
        <h2>Financial Situation</h2>
        <p class="subtitle">Help us understand your financial profile.</p>

        <KycBadge v-if="kycStatus" :kyc="kycStatus" />

        <button type="button" class="btn btn-demo" @click="fillPage3">Auto-fill demo data</button>
        <form @submit.prevent="submitPage(3)">
          <div class="form-grid">
            <div class="form-group">
              <label for="employmentType">Employment Type</label>
              <select id="employmentType" v-model="page3.employmentType" required>
                <option value="Employed">Employed</option>
                <option value="SelfEmployed">Self-Employed</option>
                <option value="Freelance">Freelance</option>
                <option value="Retired">Retired</option>
                <option value="Student">Student</option>
                <option value="Unemployed">Unemployed</option>
              </select>
            </div>
            <div class="form-group">
              <label for="employer">Employer</label>
              <input id="employer" v-model="page3.employer" required placeholder="Temporal Technologies" />
            </div>
            <div class="form-group">
              <label for="monthlyIncome">Monthly Income (&euro;)</label>
              <input id="monthlyIncome" v-model.number="page3.monthlyIncome" type="number" min="0" step="100" required />
            </div>
            <div class="form-group">
              <label for="monthlyExpenses">Monthly Expenses (&euro;)</label>
              <input id="monthlyExpenses" v-model.number="page3.monthlyExpenses" type="number" min="0" step="100" required />
            </div>
          </div>
          <div class="form-actions">
            <button type="button" class="btn btn-secondary" :disabled="submitting" @click="goToPage(2)">
              Previous
            </button>
            <button type="submit" class="btn btn-primary" :disabled="submitting">
              <span v-if="submitting" class="spinner" />
              {{ submitting ? 'Saving...' : 'Continue' }}
            </button>
          </div>
        </form>
      </div>

      <!-- ── Page 4: Summary ── -->
      <div v-if="currentPage === 4" class="form-card">
        <h2>Review Your Application</h2>
        <p class="subtitle">Please review your information before submitting.</p>

        <KycBadge v-if="kycStatus" :kyc="kycStatus" show-reason />

        <!-- Personal Info Summary -->
        <div v-if="pageData.page1" class="summary-section">
          <h3>Personal Identity</h3>
          <div class="summary-row">
            <span class="label">Name</span>
            <span class="value">{{ pageData.page1.firstName }} {{ pageData.page1.lastName }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Date of Birth</span>
            <span class="value">{{ pageData.page1.dateOfBirth }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Nationality</span>
            <span class="value">{{ pageData.page1.nationality }}</span>
          </div>
          <div class="summary-row">
            <span class="label">ID</span>
            <span class="value">{{ humanize(pageData.page1.idType) }} — {{ pageData.page1.idNumber }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Email</span>
            <span class="value">{{ pageData.page1.email }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Phone</span>
            <span class="value">{{ pageData.page1.phone }}</span>
          </div>
        </div>

        <!-- Address Summary -->
        <div v-if="pageData.page2" class="summary-section">
          <h3>Residential Address</h3>
          <div class="summary-row">
            <span class="label">Street</span>
            <span class="value">{{ pageData.page2.street }}</span>
          </div>
          <div class="summary-row">
            <span class="label">City</span>
            <span class="value">{{ pageData.page2.postalCode }} {{ pageData.page2.city }}</span>
          </div>
          <div v-if="pageData.page2.stateProvince" class="summary-row">
            <span class="label">State / Province</span>
            <span class="value">{{ pageData.page2.stateProvince }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Country</span>
            <span class="value">{{ pageData.page2.country }}</span>
          </div>
        </div>

        <!-- Financial Summary -->
        <div v-if="pageData.page3" class="summary-section">
          <h3>Financial Situation</h3>
          <div class="summary-row">
            <span class="label">Employment</span>
            <span class="value">{{ humanize(pageData.page3.employmentType) }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Employer</span>
            <span class="value">{{ pageData.page3.employer }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Monthly Income</span>
            <span class="value">{{ formatCurrency(pageData.page3.monthlyIncome) }}</span>
          </div>
          <div class="summary-row">
            <span class="label">Monthly Expenses</span>
            <span class="value">{{ formatCurrency(pageData.page3.monthlyExpenses) }}</span>
          </div>
        </div>

        <!-- Terms & Submit -->
        <div class="checkbox-group">
          <input id="terms" v-model="termsAccepted" type="checkbox" />
          <label for="terms">
            I confirm that the information provided is accurate and complete. I agree to the
            NovaBank Terms of Service and Privacy Policy.
          </label>
        </div>

        <div class="form-actions">
          <button type="button" class="btn btn-secondary" :disabled="submitting" @click="goToPage(3)">
            Previous
          </button>
          <button
            class="btn btn-primary btn-lg"
            :disabled="!termsAccepted || submitting"
            @click="submitFinal"
          >
            <span v-if="submitting" class="spinner" />
            {{ submitting ? 'Submitting...' : 'Submit Application' }}
          </button>
        </div>
      </div>
      </template>
    </main>
  </div>
</template>

<style scoped>
.confirmation-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  font-size: 2rem;
}

.confirmation-icon.status-pending {
  background: #fef9e7;
  color: #b7950b;
}

.confirmation-icon.status-approved {
  background: #eafaf1;
  color: var(--green);
}

.confirmation-icon.status-rejected,
.confirmation-icon.status-abandoned {
  background: #fdedec;
  color: var(--red);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  border-radius: 24px;
  font-size: 0.875rem;
  font-weight: 600;
  margin: 20px 0;
}

.status-badge.status-pending {
  background: #fef9e7;
  color: #b7950b;
}

.status-badge.status-approved {
  background: #eafaf1;
  color: var(--green);
}

.status-badge.status-rejected,
.status-badge.status-abandoned {
  background: #fdedec;
  color: var(--red);
}

.review-reason {
  margin-top: 8px;
  padding: 12px 16px;
  background: var(--paper);
  border-radius: var(--radius);
  font-size: 0.875rem;
  color: var(--ink-light);
  max-width: 400px;
  margin-left: auto;
  margin-right: auto;
}

.hint {
  font-size: 0.8125rem;
  color: var(--gray);
  margin-top: 8px;
}
</style>
