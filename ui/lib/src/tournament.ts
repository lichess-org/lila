import { storage } from './storage';

const lastRedirectStorage = storage.make('last-redirect');

export function redirectFirst(gameId: string, rightNow?: boolean): void {
  const delay = rightNow || document.hasFocus() ? 10 : 1000 + Math.random() * 500;
  setTimeout(() => {
    if (lastRedirectStorage.get() !== gameId) {
      lastRedirectStorage.set(gameId);
      site.redirect('/' + gameId, true);
    }
  }, delay);
}
