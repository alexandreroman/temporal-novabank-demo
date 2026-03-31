<script setup lang="ts">
defineProps<{
  kyc: { status: string; reason?: string }
  showReason?: boolean
}>()
</script>

<template>
  <div style="margin-bottom: 24px; text-align: center;">
    <div
      class="kyc-badge"
      :class="{
        pending: kyc.status === 'Pending',
        approved: kyc.status === 'Approved',
        failed: kyc.status === 'Failed',
      }"
    >
      <span v-if="kyc.status === 'Pending'" class="spinner" />
      <template v-if="kyc.status === 'Pending'">KYC Verification in progress...</template>
      <template v-else-if="kyc.status === 'Approved'">&#10003; Identity Verified</template>
      <template v-else-if="kyc.status === 'Failed'">
        &#10007; Verification Failed<template v-if="showReason && kyc.reason"> — {{ kyc.reason }}</template>
      </template>
    </div>
  </div>
</template>
