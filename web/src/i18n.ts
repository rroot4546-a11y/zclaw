import { ref } from 'vue'

export type SupportedLang = 'en' | 'ar'

export type I18nTable = Record<string, string>

const STORAGE_KEY = 'zclaw.lang'

export const en: I18nTable = {
  brandName: 'zclaw',
  hero: "Let's build",
  searchThreads: 'Search threads',
  filterThreads: 'Filter threads...',
  clearSearch: 'Clear search',
  newThread: 'New thread',
  expandSidebar: 'Expand sidebar',
  collapseSidebar: 'Collapse sidebar',
  automations: 'Automations',
  skills: 'Skills',
  threads: 'Threads',
  noMatchingThreads: 'No matching threads',
  loadingThreads: 'Loading threads...',
  projectName: 'Project name',
  noThreads: 'No threads yet',
  confirm: 'Confirm',
  composerPlaceholder: 'Type a message...',
  composerSelectThread: 'Select a thread to send a message',
  chooseAThread: 'Choose a thread',
  model: 'Model',
  thinking: 'Thinking',
  dropdownEmpty: 'No models yet — open Settings and connect a provider',
  stop: 'Stop',
  send: 'Send',
  rNone: 'None',
  rMinimal: 'Minimal',
  rLow: 'Low',
  rMedium: 'Medium',
  rHigh: 'High',
  rExtraHigh: 'Extra high',
  loadingMessages: 'Loading messages...',
  noMessagesYet: 'No messages in this thread yet.',
  accept: 'Accept',
  acceptSession: 'Accept for Session',
  decline: 'Decline',
  cancel: 'Cancel',
  submitAnswers: 'Submit Answers',
  toolFailure: 'Fail Tool Call',
  toolSuccess: 'Success (Empty)',
  returnEmpty: 'Return Empty Result',
  rejectRequest: 'Reject Request',
  otherAnswer: 'Other answer',
  closeImagePreview: 'Close image preview',
  noTypesYet: 'No types yet',
  apiPanelTitle: 'AppServer API',
  apiPanelLoading: 'Loading method catalog...',
  settings: 'Settings',
  language: 'Language',
  themeLabel: 'Theme',
  themeDark: 'Dark',
  themeLight: 'Light',
  fontSize: 'Text size',
  fSmall: 'Small',
  fMedium: 'Medium',
  fLarge: 'Large',
  about: 'About',
  appVersion: 'zclaw v1.0.0',
  closeBtn: 'Close',
  chooseFolder: 'Choose folder',
  newThreadIn: 'Start new thread in',
  pinAction: 'Pin',
  archiveAction: 'Archive',
  projectMenu: 'Project',
  autoRefreshIn: 'Auto refresh in',
  enableAutoRefresh: 'Enable 4s refresh',
}

export const ar: I18nTable = {
  brandName: 'zclaw',
  hero: 'لنبنِ معاً',
  searchThreads: 'البحث في المحادثات',
  filterThreads: 'تصفية المحادثات…',
  clearSearch: 'مسح البحث',
  newThread: 'محادثة جديدة',
  expandSidebar: 'توسيع الشريط الجانبي',
  collapseSidebar: 'طي الشريط الجانبي',
  automations: 'الأتمتة',
  skills: 'المهارات',
  threads: 'المحادثات',
  noMatchingThreads: 'لا توجد نتائج مطابقة',
  loadingThreads: 'جارٍ تحميل المحادثات…',
  projectName: 'اسم المشروع',
  noThreads: 'لا توجد محادثات بعد',
  confirm: 'تأكيد',
  composerPlaceholder: 'اكتب رسالتك…',
  composerSelectThread: 'اختر محادثةً للإرسال',
  chooseAThread: 'اختر محادثةً',
  model: 'النموذج',
  thinking: 'التفكير',
  dropdownEmpty: 'لا توجد مودلات بعد — افتح الإعدادات واربط موفّراً',
  stop: 'إيقاف',
  send: 'إرسال',
  rNone: 'بدون',
  rMinimal: 'بسيط',
  rLow: 'منخفض',
  rMedium: 'متوسط',
  rHigh: 'مرتفع',
  rExtraHigh: 'مرتفع جداً',
  loadingMessages: 'جارٍ تحميل الرسائل…',
  noMessagesYet: 'لا توجد رسائل بعد في هذه المحادثة.',
  accept: 'قبول',
  acceptSession: 'قبول للجلسة',
  decline: 'رفض',
  cancel: 'إلغاء',
  submitAnswers: 'إرسال الإجابات',
  toolFailure: 'فشل استدعاء الأداة',
  toolSuccess: 'نجاح (فارغ)',
  returnEmpty: 'إرجاع نتيجة فارغة',
  rejectRequest: 'رفض الطلب',
  otherAnswer: 'إجابة أخرى',
  closeImagePreview: 'إغلاق معاينة الصورة',
  noTypesYet: 'لا توجد أنواع بعد',
  apiPanelTitle: 'التحكم عبر AppServer',
  apiPanelLoading: 'جارٍ تحميل…',
  settings: 'الإعدادات',
  language: 'اللغة',
  themeLabel: 'المظهر',
  themeDark: 'داكن',
  themeLight: 'فاتح',
  fontSize: 'حجم الخط',
  fSmall: 'صغير',
  fMedium: 'متوسط',
  fLarge: 'كبير',
  about: 'حول التطبيق',
  appVersion: 'zclaw v1.0.0',
  closeBtn: 'إغلاق',
  chooseFolder: 'اختر مجلّد',
  newThreadIn: 'محادثة جديدة ضمن',
  pinAction: 'تثبيت',
  archiveAction: 'أرشفة',
  projectMenu: 'المشروع',
  autoRefreshIn: 'تحديث تلقائي بعد',
  enableAutoRefresh: 'تفعيل التحديث كل 4 ثوانٍ',
}

const TABLES: Record<SupportedLang, I18nTable> = { en, ar }

export const currentLang = ref<SupportedLang>(loadLang())

function loadLang(): SupportedLang {
  if (typeof window === 'undefined') return 'en'
  try {
    return window.localStorage.getItem(STORAGE_KEY) === 'ar' ? 'ar' : 'en'
  } catch {
    return 'en'
  }
}

export function setLang(lang: SupportedLang): void {
  currentLang.value = lang
  if (typeof window !== 'undefined') {
    try {
      window.localStorage.setItem(STORAGE_KEY, lang)
    } catch {
      /* ignore */
    }
  }
  applyLang()
}

export function t(key: string): string {
  const table = TABLES[currentLang.value] ?? en
  return table[key] ?? en[key] ?? key
}

export function applyLang(): void {
  if (typeof document === 'undefined') return
  document.documentElement.lang = currentLang.value
  document.documentElement.dir = currentLang.value === 'ar' ? 'rtl' : 'ltr'
}

applyLang()