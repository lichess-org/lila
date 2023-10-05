export const currentTheme = () => {
  const dataTheme = document.body.dataset.theme!;
  if (dataTheme === 'system')
    return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
  else if (dataTheme === 'light') return 'light';
  else return 'dark';
};
export const supportsSystemTheme = () =>
  window.matchMedia('(prefers-color-scheme: light)').media !== 'not all';
