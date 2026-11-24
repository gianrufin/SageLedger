import { ThemeModePreference } from '../types/ledger';

const THEME_MODE_KEY = 'sageledger_theme_mode';
const ONBOARDING_COMPLETE_KEY = 'sageledger_onboarding_complete';
const DISMISSED_RELEASE_TAG_KEY = 'sageledger_dismissed_release_tag';

export function loadThemeMode(): ThemeModePreference {
  try {
    const raw = window.localStorage.getItem(THEME_MODE_KEY);
    if (
      raw === ThemeModePreference.LIGHT ||
      raw === ThemeModePreference.DARK ||
      raw === ThemeModePreference.SYSTEM
    ) {
      return raw;
    }
  } catch {
    // Ignore storage read errors in restricted environments
  }
  return ThemeModePreference.DARK;
}

export function saveThemeMode(mode: ThemeModePreference): void {
  try {
    window.localStorage.setItem(THEME_MODE_KEY, mode);
  } catch {
    // Ignore storage write errors
  }
}

export function loadOnboardingComplete(): boolean {
  try {
    return window.localStorage.getItem(ONBOARDING_COMPLETE_KEY) === 'true';
  } catch {
    return false;
  }
}

export function saveOnboardingComplete(complete: boolean): void {
  try {
    window.localStorage.setItem(ONBOARDING_COMPLETE_KEY, complete ? 'true' : 'false');
  } catch {
    // Ignore storage write errors
  }
}

export function loadDismissedReleaseTag(): string | null {
  try {
    return window.localStorage.getItem(DISMISSED_RELEASE_TAG_KEY);
  } catch {
    return null;
  }
}

export function saveDismissedReleaseTag(tag: string): void {
  try {
    window.localStorage.setItem(DISMISSED_RELEASE_TAG_KEY, tag);
  } catch {
    // Ignore storage write errors
  }
}

export function applyThemeClass(mode: ThemeModePreference): boolean {
  const prefersDark =
    typeof window !== 'undefined' &&
    window.matchMedia?.('(prefers-color-scheme: dark)').matches;
  const isDark =
    mode === ThemeModePreference.DARK ||
    (mode === ThemeModePreference.SYSTEM && prefersDark);

  if (typeof document !== 'undefined') {
    document.documentElement.classList.toggle('dark', isDark);
  }
  return isDark;
}
