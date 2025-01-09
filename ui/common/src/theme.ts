export const currentTheme = (): 'light' | 'dark' => {
  const dataTheme = document.body.dataset.theme!;
  if (dataTheme === 'light') return 'light';
  else return 'dark'; // and transp
};
