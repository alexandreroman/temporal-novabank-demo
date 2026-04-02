<script setup lang="ts">
const loading = ref(false)

async function startApplication() {
  loading.value = true
  try {
    const { applicationId } = await $fetch('/api/applications/start', { method: 'POST' })
    await navigateTo(`/applications/${applicationId}`)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <header class="site-header">
      <div class="container">
        <span class="logo">NovaBank</span>
        <nav class="header-nav">
          <a href="https://temporal.io" target="_blank">Powered by <img src="~/assets/images/temporal-logo.png" alt="Temporal" class="temporal-logo"></a>
        </nav>
      </div>
    </header>

    <section class="hero">
      <h1>Your new account,<br><span>in minutes</span></h1>
      <p>Open a NovaBank account entirely online. Our streamlined process uses durable execution so your progress is never lost.</p>
      <button class="btn btn-primary btn-lg" :disabled="loading" @click="startApplication">
        <span v-if="loading" class="spinner" />
        {{ loading ? 'Starting...' : 'Open an Account' }}
      </button>
    </section>

    <section class="features">
      <div class="feature">
        <div class="feature-icon"><svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg></div>
        <h3>Instant KYC</h3>
        <p>Identity verification runs in the background while you continue filling your application.</p>
      </div>
      <div class="feature">
        <div class="feature-icon"><svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><polyline points="9 12 11 14 15 10"/></svg></div>
        <h3>Never Lose Progress</h3>
        <p>Powered by Temporal durable execution. Close your browser, come back later — your data is safe.</p>
      </div>
      <div class="feature">
        <div class="feature-icon"><svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg></div>
        <h3>Fast Approval</h3>
        <p>Our compliance team reviews applications in real time with a human-in-the-loop workflow.</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.feature-icon {
  color: var(--gold);
}
</style>
