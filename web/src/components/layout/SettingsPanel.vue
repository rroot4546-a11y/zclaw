<template>
  <div class="settings-root">
    <button
      type="button"
      class="settings-trigger"
      :aria-label="t('settings')"
      :title="t('settings')"
      @click="isOpen = !isOpen"
    >
      <IconTablerSettings class="settings-trigger-icon" />
    </button>

    <div v-if="isOpen" class="settings-panel">
      <div class="settings-panel-header">
        <span class="settings-panel-title">{{ t('settings') }}</span>
        <button type="button" class="settings-panel-close" :aria-label="t('closeBtn')" @click="isOpen = false">
          <IconTablerX class="settings-panel-close-icon" />
        </button>
      </div>

      <div class="settings-section">
        <span class="settings-label">{{ t('language') }}</span>
        <div class="settings-segmented">
          <button
            type="button"
            class="settings-segmented-item"
            :class="{ 'settings-segmented-item-active': currentLang === 'en' }"
            @click="onLang('en')"
          >
            English
          </button>
          <button
            type="button"
            class="settings-segmented-item"
            :class="{ 'settings-segmented-item-active': currentLang === 'ar' }"
            @click="onLang('ar')"
          >
            العربية
          </button>
        </div>
      </div>

      <div class="settings-section">
        <span class="settings-label">{{ t('themeLabel') }}</span>
        <div class="settings-segmented">
          <button
            type="button"
            class="settings-segmented-item"
            :class="{ 'settings-segmented-item-active': theme === 'dark' }"
            @click="setTheme('dark')"
          >
            {{ t('themeDark') }}
          </button>
          <button
            type="button"
            class="settings-segmented-item"
            :class="{ 'settings-segmented-item-active': theme === 'light' }"
            @click="setTheme('light')"
          >
            {{ t('themeLight') }}
          </button>
        </div>
      </div>

      <div class="settings-section">
        <span class="settings-label">{{ t('fontSize') }}</span>
        <div class="settings-segmented">
          <button
            type="button"
            class="settings-segmented-item"
            :class="{ 'settings-segmented-item-active': fontSize === 'sm' }"
            @click="setFontSize('sm')"
          >
            {{ t('fSmall') }}
          </button>
          <button
            type="button"
            class="settings-segmented-item"
            :class="{ 'settings-segmented-item-active': fontSize === 'md' }"
            @click="setFontSize('md')"
          >
            {{ t('fMedium') }}
          </button>
          <button
            type="button"
            class="settings-segmented-item"
            :class="{ 'settings-segmented-item-active': fontSize === 'lg' }"
            @click="setFontSize('lg')"
          >
            {{ t('fLarge') }}
          </button>
        </div>
      </div>

      <div class="settings-section settings-section-about">
        <span class="settings-label">{{ t('about') }}</span>
        <span class="settings-about-value">{{ t('appVersion') }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { currentLang, setLang, t } from '../../i18n'
import { useSettings } from '../../composables/useSettings'
import IconTablerSettings from '../icons/IconTablerSettings.vue'
import IconTablerX from '../icons/IconTablerX.vue'

const { theme, fontSize, setTheme, setFontSize } = useSettings()

const isOpen = ref(false)

function onLang(lang: 'en' | 'ar'): void {
  setLang(lang)
}
</script>

<style scoped>
@reference "tailwindcss";

.settings-root {
  @apply relative;
}

.settings-trigger {
  @apply inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full border-0 bg-transparent text-zinc-500 transition hover:bg-zinc-100 hover:text-zinc-800;
}

.settings-trigger-icon {
  @apply h-5 w-5;
}

.settings-panel {
  @apply absolute z-50 mt-2 w-64 rounded-2xl border border-zinc-200 bg-white p-3 shadow-xl;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
}

html[dir='rtl'] .settings-panel {
  transform: translateX(50%);
}

.settings-panel-header {
  @apply mb-2 flex items-center justify-between;
}

.settings-panel-title {
  @apply text-sm font-semibold text-zinc-800;
}

.settings-panel-close {
  @apply inline-flex h-7 w-7 items-center justify-center rounded-full text-zinc-400 transition hover:bg-zinc-100 hover:text-zinc-700;
}

.settings-panel-close-icon {
  @apply h-4 w-4;
}

.settings-section {
  @apply mb-3;
}

.settings-label {
  @apply mb-1 block text-xs font-medium text-zinc-500;
}

.settings-segmented {
  @apply flex gap-1 rounded-xl bg-zinc-100 p-1;
}

.settings-segmented-item {
  @apply flex-1 rounded-lg border-0 px-2 py-1.5 text-xs font-medium text-zinc-600 transition;
}

.settings-segmented-item:hover {
  @apply bg-white text-zinc-900;
}

.settings-segmented-item-active {
  @apply bg-zinc-900 text-white shadow-sm;
}

.settings-segmented-item-active:hover {
  @apply bg-zinc-900 text-white;
}

.settings-section-about {
  @apply mb-0 flex items-center justify-between border-t border-zinc-100 pt-3;
}

.settings-about-value {
  @apply text-xs text-zinc-400;
}

html[data-theme='dark'] .settings-trigger {
  @apply text-zinc-400;
}

html[data-theme='dark'] .settings-trigger:hover {
  @apply bg-zinc-800 text-zinc-100;
}

html[data-theme='dark'] .settings-panel {
  @apply border-zinc-800 bg-zinc-900;
}

html[data-theme='dark'] .settings-panel-title {
  @apply text-zinc-100;
}

html[data-theme='dark'] .settings-panel-close {
  @apply text-zinc-500;
}

html[data-theme='dark'] .settings-panel-close:hover {
  @apply bg-zinc-800 text-zinc-200;
}

html[data-theme='dark'] .settings-label {
  @apply text-zinc-400;
}

html[data-theme='dark'] .settings-segmented {
  @apply bg-zinc-800;
}

html[data-theme='dark'] .settings-segmented-item {
  @apply text-zinc-300;
}

html[data-theme='dark'] .settings-segmented-item:hover {
  @apply bg-zinc-900 text-zinc-100;
}

html[data-theme='dark'] .settings-segmented-item-active,
html[data-theme='dark'] .settings-segmented-item-active:hover {
  @apply bg-zinc-100 text-zinc-900;
}

html[data-theme='dark'] .settings-section-about {
  @apply border-zinc-800;
}

html[data-theme='dark'] .settings-about-value {
  @apply text-zinc-500;
}
</style>