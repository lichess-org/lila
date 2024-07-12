export const randomToken = (): string => {
  try {
    const data = window.crypto.getRandomValues(new Uint8Array(9));
    return btoa(String.fromCharCode(...data)).replace(/[/+]/g, '_');
  } catch (_) {
    return Math.random().toString(36).slice(2, 12);
  }
};
