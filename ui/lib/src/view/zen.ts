import { throttlePromiseDelay } from '@/async';
import * as xhr from '@/xhr';

export const setZen: (zen: boolean) => Promise<void> = throttlePromiseDelay(
  () => 1000,
  zen =>
    xhr.text('/pref/zen', {
      method: 'post',
      body: xhr.form({ zen: zen ? 1 : 0 }),
    }),
);

export function toggleZenMode({ unconditional }: { unconditional?: boolean } = {}): void {
  const $body = $('body');
  const zen = $body.toggleClass('zen').hasClass('zen');
  window.dispatchEvent(new Event('resize'));
  if (unconditional || !$body.hasClass('zen-auto')) {
    setZen(zen);
  }
}
