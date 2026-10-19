<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
import ToastStack from '@/components/ToastStack.vue'
</script>

<template>
  <a class="skip-link" href="#main">Pular para o conteúdo</a>
  <AppHeader />
  <main id="main" class="container">
    <RouterView />
  </main>
  <ToastStack />
</template>
