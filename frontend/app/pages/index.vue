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
        <div class="feature-icon">&#9889;</div>
        <h3>Instant KYC</h3>
        <p>Identity verification runs in the background while you continue filling your application.</p>
      </div>
      <div class="feature">
        <div class="feature-icon">&#128274;</div>
        <h3>Never Lose Progress</h3>
        <p>Powered by Temporal durable execution. Close your browser, come back later — your data is safe.</p>
      </div>
      <div class="feature">
        <div class="feature-icon">&#9989;</div>
        <h3>Fast Approval</h3>
        <p>Our compliance team reviews applications in real time with a human-in-the-loop workflow.</p>
      </div>
    </section>
  </div>
</template>
