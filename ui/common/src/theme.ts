export const prefersLight = (): MediaQueryList => window.matchMedia('(prefers-color-scheme: light)');

export const currentTheme = () => {
  const dataTheme = document.body.dataset.theme!;
  if (dataTheme === 'system') return prefersLight().matches ? 'light' : 'dark';
  else if (dataTheme === 'light') return 'light';
  else return 'dark';
};

export const supportsSystemTheme = () => prefersLight().media !== 'not all';
