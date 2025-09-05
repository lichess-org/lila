export const openInApp = (url: string): string => {
  const parsed = URL.parse(url);

  if (parsed) {
    return 'lichess-broadcaster:/' + parsed.pathname;
  }

  throw new Error('Cannot parse URL');
};
