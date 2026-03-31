<template>
  <div>
    <header class="site-header">
      <div class="container">
        <NuxtLink to="/" class="logo">NovaBank<span class="logo-suffix">Backoffice</span></NuxtLink>
        <nav class="header-nav">
          <a href="https://temporal.io" target="_blank">Powered by <img src="~/assets/images/temporal-logo.png" alt="Temporal" class="temporal-logo"></a>
        </nav>
      </div>
    </header>
    <NuxtRouteAnnouncer />
    <NuxtPage />
  </div>
</template>
