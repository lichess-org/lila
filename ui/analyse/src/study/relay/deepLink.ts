export const broadcasterDeepLink = (url: string): string => {
  const parsed = new URL(url);
  return 'lichess-broadcaster:/' + parsed.pathname;
};
