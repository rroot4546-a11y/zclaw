<template>
  <nav class="sidebar-primary-nav">
    <SidebarMenuRow as="button" class="sidebar-nav-item" type="button">{{ t('newThread') }}</SidebarMenuRow>
    <SidebarMenuRow as="button" class="sidebar-nav-item" type="button">{{ t('automations') }}</SidebarMenuRow>
    <SidebarMenuRow as="button" class="sidebar-nav-item" type="button">{{ t('skills') }}</SidebarMenuRow>
  </nav>
</template>

<script setup lang="ts">
import { t } from '../../i18n'
import SidebarMenuRow from './SidebarMenuRow.vue'
</script>

<style scoped>
@reference "tailwindcss";

.sidebar-primary-nav {
  @apply flex flex-col gap-1;
}

.sidebar-nav-item {
  @apply text-sm font-normal text-slate-700 hover:bg-slate-200;
}
</style>
