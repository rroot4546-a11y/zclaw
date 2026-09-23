<template>
  <div class="engine-bar">
    <button
      type="button"
      class="engine-bar-home"
      :aria-label="t('engineHome')"
      :title="t('engineHome')"
      @click="goHome"
    >
      <IconTablerChevronLeft class="engine-bar-home-icon" />
      <span class="engine-bar-home-label">{{ t('engineHome') }}</span>
    </button>

    <div class="engine-bar-model">
      <ComposerDropdown
        class="engine-bar-model-dropdown"
        :model-value="selectedModel"
        :options="modelOptions"
        :placeholder="t('model')"
        :disabled="disabled"
        open-direction="down"
        @update:model-value="onModel"
      >
      </ComposerDropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { t } from '../../i18n'
import IconTablerChevronLeft from '../icons/IconTablerChevronLeft.vue'
import ComposerDropdown from '../content/ComposerDropdown.vue'

const props = defineProps<{
  models: string[]
  selectedModel: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:selected-model': [modelId: string]
}>()

const modelOptions = computed(() =>
  props.models.map((modelId) => ({ value: modelId, label: modelId })),
)

function onModel(value: string): void {
  emit('update:selected-model', value)
}

function goHome(): void {
  // On Android the native engine picker intercepts this scheme; elsewhere it
  // degrades to a no-op so the SPA is unaffected.
  try {
    window.location.href = 'zclaw://engines'
  } catch {
    // ignore
  }
}
</script>

<style scoped>
@reference "tailwindcss";

.engine-bar {
  @apply flex items-center gap-1.5;
}

.engine-bar-home {
  @apply inline-flex h-8 items-center gap-0.5 rounded-full border border-zinc-300 bg-white px-2 text-xs font-semibold text-zinc-700 transition hover:bg-zinc-100 select-none;
}

.engine-bar-home-icon {
  @apply h-4 w-4 shrink-0;
}

.engine-bar-home-label {
  @apply whitespace-nowrap hidden;
}

@media (min-width: 640px) {
  .engine-bar-home-label {
    @apply inline;
  }
}

.engine-bar-model .composer-dropdown-trigger {
  @apply h-8 rounded-full border border-zinc-300 bg-white px-2.5 text-xs font-semibold text-zinc-800;
}

.engine-bar-model .composer-dropdown-prefix {
  @apply font-semibold text-zinc-500;
}

.engine-bar-model .composer-dropdown-value {
  @apply text-zinc-800;
}

html[data-theme='dark'] .engine-bar-home {
  @apply border-zinc-700 bg-slate-800 text-zinc-200 hover:bg-slate-700;
}

html[data-theme='dark'] .engine-bar-model .composer-dropdown-trigger {
  @apply border-zinc-700 bg-slate-800 text-zinc-100;
}

html[data-theme='dark'] .engine-bar-model .composer-dropdown-prefix {
  @apply text-zinc-400;
}

html[data-theme='dark'] .engine-bar-model .composer-dropdown-value {
  @apply text-zinc-100;
}
</style>