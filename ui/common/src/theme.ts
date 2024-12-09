export const prefersLight = (): MediaQueryList => window.matchMedia('(prefers-color-scheme: light)');

export const currentTheme = (): 'light' | 'dark' => {
  const dataTheme = document.body.dataset.theme!;
  if (dataTheme === 'system') return prefersLight().matches ? 'light' : 'dark';
  else if (dataTheme === 'light') return 'light';
  else return 'dark';
};

export const supportsSystemTheme = (): boolean => prefersLight().media !== 'not all';
