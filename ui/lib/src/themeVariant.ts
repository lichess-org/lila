export type ThemeVariant = 'default' | 'yellow' | 'blue';

const storageKey = 'theme.variant';

export function getThemeVariant(): ThemeVariant {
  const variant = localStorage.getItem(storageKey);
  return variant === 'yellow' || variant === 'blue' ? variant : 'default';
}

export function setThemeVariant(variant: ThemeVariant): void {
  if (variant === 'default') localStorage.removeItem(storageKey);
  else localStorage.setItem(storageKey, variant);
  applyThemeVariant(variant);
}

export function applyThemeVariant(variant: ThemeVariant = getThemeVariant()): void {
  const root = document.documentElement;
  root.classList.remove('theme-yellow', 'theme-blue');
  if (variant !== 'default') root.classList.add(`theme-${variant}`);
}
