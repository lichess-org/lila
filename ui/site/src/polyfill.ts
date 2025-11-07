import { pubsub } from 'lib/pubsub';

export async function loadPolyfills(): Promise<void> {
  site.polyfill = {} as typeof site.polyfill;
  await Promise.all([dialogPolyfill(), resizePolyfill()]);
}

async function dialogPolyfill() {
  site.polyfill.registerDialog =
    typeof window.HTMLDialogElement === 'undefined'
      ? () => {}
      : (await import(site.asset.url('npm/dialog-polyfill.esm.js')).catch(() => undefined))?.registerDialog;
  pubsub.complete('polyfill.dialog');
}

async function resizePolyfill() {
  if (typeof window.ResizeObserver === 'undefined') {
    window.ResizeObserver = (await import('@juggle/resize-observer')).ResizeObserver;
  }
}
