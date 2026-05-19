import { storage } from './storage';

const lastRedirect = storage.make('last-redirect');

export function redirectFirst(gameId: string, rightNow?: boolean): void {
  const delay = rightNow || document.hasFocus() ? 10 : 1000 + Math.random() * 500;
  setTimeout(() => {
    if (lastRedirect.get() !== gameId) {
      lastRedirect.set(gameId);
      site.redirect('/' + gameId, true);
    }
  }, delay);
}
