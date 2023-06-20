export const withLang = (path: string): string => {
  if (document.body.hasAttribute('data-user')) return path;
  const language = document.documentElement.lang.slice(0, 2);
  return language == 'en' ? path : `/${language}${path}`;
};
