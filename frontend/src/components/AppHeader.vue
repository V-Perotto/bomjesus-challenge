<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

// Destaca a aba também nas telas de cadastro e edição (rotas irmãs da lista)
const isSection = (prefix: string) => route.path === prefix || route.path.startsWith(`${prefix}/`)
</script>

<template>
  <header class="app-header">
    <div class="container app-header__inner">
      <span class="app-header__brand">Bom Jesus - Challenge</span>
      <nav class="app-header__nav" aria-label="Seções">
        <RouterLink
          to="/students"
          class="app-header__tab"
          :class="{ 'is-active': isSection('/students') }"
          :aria-current="isSection('/students') ? 'page' : undefined"
        >
          Alunos
        </RouterLink>
        <RouterLink
          to="/professors"
          class="app-header__tab"
          :class="{ 'is-active': isSection('/professors') }"
          :aria-current="isSection('/professors') ? 'page' : undefined"
        >
          Professores
        </RouterLink>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  background: var(--color-surface);
  border-bottom: 4px solid var(--color-brand-lime);
}

.app-header__inner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  min-height: 64px;
  padding-top: var(--space-2);
  padding-bottom: var(--space-2);
}

.app-header__brand {
  font-weight: 800;
  font-size: 1.1rem;
  color: var(--color-primary);
}

.app-header__nav {
  display: flex;
  gap: var(--space-2);
}

.app-header__tab {
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-sm);
  color: var(--color-text);
  font-weight: 600;
  text-decoration: none;
}

.app-header__tab:hover {
  background: var(--color-neutral-soft);
}

.app-header__tab.is-active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
  box-shadow: inset 0 -3px 0 var(--color-brand-lime);
}
</style>
