export const openInApp = (url: string): string => {
  try {
    const parsed = new URL(url);
    return 'lichess-broadcaster:/' + parsed.pathname;
  } catch (e) {
    throw new Error('Cannot parse URL');
  }
};
