import { ref } from 'vue'

export type ThemeMode = 'dark' | 'light'
export type FontSizeMode = 'sm' | 'md' | 'lg'

const THEME_KEY = 'zclaw.theme'
const FONT_KEY = 'zclaw.fontsize'

const theme = ref<ThemeMode>(loadTheme())

const fontSize = ref<FontSizeMode>(loadFontSize())

function loadTheme(): ThemeMode {
  if (typeof window === 'undefined') return 'dark'
  try {
    return window.localStorage.getItem(THEME_KEY) === 'light' ? 'light' : 'dark'
  } catch {
    return 'dark'
  }
}

function loadFontSize(): FontSizeMode {
  if (typeof window === 'undefined') return 'md'
  try {
    const raw = window.localStorage.getItem(FONT_KEY)
    return raw === 'sm' || raw === 'lg' ? raw : 'md'
  } catch {
    return 'md'
  }
}

function applyTheme(): void {
  if (typeof document === 'undefined') return
  document.documentElement.dataset.theme = theme.value
}

function applyFontSize(): void {
  if (typeof document === 'undefined') return
  const scale = fontSize.value === 'sm' ? '0.9rem' : fontSize.value === 'lg' ? '1.1rem' : '1rem'
  document.documentElement.style.fontSize = scale
}

export function setTheme(mode: ThemeMode): void {
  theme.value = mode
  if (typeof window !== 'undefined') {
    try {
      window.localStorage.setItem(THEME_KEY, mode)
    } catch {
      /* ignore */
    }
  }
  applyTheme()
}

export function setFontSize(mode: FontSizeMode): void {
  fontSize.value = mode
  if (typeof window !== 'undefined') {
    try {
      window.localStorage.setItem(FONT_KEY, mode)
    } catch {
      /* ignore */
    }
  }
  applyFontSize()
}

applyTheme()
applyFontSize()

export function useSettings(): {
  theme: typeof theme
  fontSize: typeof fontSize
  setTheme: typeof setTheme
  setFontSize: typeof setFontSize
} {
  return { theme, fontSize, setTheme, setFontSize }
}