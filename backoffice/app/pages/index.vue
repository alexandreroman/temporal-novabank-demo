<script setup lang="ts">
const activeFilter = ref('')

const { data: applications, refresh } = useFetch('/api/applications', {
  query: computed(() => activeFilter.value ? { status: activeFilter.value } : {}),
})

const refreshInterval = setInterval(() => refresh(), 5000)
onUnmounted(() => clearInterval(refreshInterval))

const allApps = computed(() => applications.value ?? [])

const stats = computed(() => {
  const apps = allApps.value as any[]
  return {
    total: apps.length,
    pendingReview: apps.filter((a: any) => a.reviewStatus === 'PendingReview').length,
    approved: apps.filter((a: any) => a.reviewStatus === 'Approved').length,
    rejected: apps.filter((a: any) => a.reviewStatus === 'Rejected').length,
  }
})

const filters = [
  { label: 'All', value: '' },
  { label: 'Pending Review', value: 'PendingReview' },
  { label: 'In Progress', value: 'InProgress' },
  { label: 'Approved', value: 'Approved' },
  { label: 'Rejected', value: 'Rejected' },
  { label: 'Abandoned', value: 'Abandoned' },
]

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

function kycClass(status: string) {
  const map: Record<string, string> = {
    Pending: 'pending',
    Approved: 'approved',
    Failed: 'failed',
  }
  return map[status] || 'pending'
}

function formatDate(iso: string) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('en-GB', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}
</script>

<template>
  <main class="container" style="padding-top: 40px; padding-bottom: 64px;">
    <h1 class="page-title">Applications</h1>
    <p class="page-subtitle">Review and manage account opening applications.</p>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">Total</div>
        <div class="stat-value">{{ stats.total }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Pending Review</div>
        <div class="stat-value">{{ stats.pendingReview }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Approved</div>
        <div class="stat-value">{{ stats.approved }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Rejected</div>
        <div class="stat-value">{{ stats.rejected }}</div>
      </div>
    </div>

    <div class="filters">
      <button
        v-for="f in filters"
        :key="f.value"
        class="filter-btn"
        :class="{ active: activeFilter === f.value }"
        @click="activeFilter = f.value"
      >
        {{ f.label }}
      </button>
    </div>

    <div class="table-card">
      <table v-if="allApps.length">
        <thead>
          <tr>
            <th>Applicant</th>
            <th>Review Status</th>
            <th>KYC</th>
            <th>Started</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="app in (allApps as any[])" :key="app.workflowId">
            <td>
              <div class="applicant-name">{{ app.applicantName }}</div>
              <div class="app-id">{{ app.applicationId }}</div>
            </td>
            <td>
              <span class="status-badge" :class="statusClass(app.reviewStatus)">
                {{ statusLabel(app.reviewStatus) }}
              </span>
            </td>
            <td>
              <span class="kyc-badge" :class="kycClass(app.kycStatus)">
                {{ app.kycStatus }}
              </span>
            </td>
            <td>{{ formatDate(app.startTime) }}</td>
            <td>
              <NuxtLink :to="`/applications/${app.applicationId}`" class="btn btn-secondary btn-sm">
                Review
              </NuxtLink>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="table-empty">
        No applications found.
      </div>
    </div>
  </main>
</template>
