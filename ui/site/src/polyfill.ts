import { pubsub } from 'lib/pubsub';

export async function loadPolyfills(): Promise<void> {
  await Promise.all([dialogPolyfill(), resizePolyfill()]);
}

async function dialogPolyfill() {
  let registerDialog = undefined;
  try {
    if (typeof window.HTMLDialogElement === 'undefined') {
      registerDialog = (await import(site.asset.url('npm/dialog-polyfill.esm.js'))).default.registerDialog;
    }
  } finally {
    pubsub.complete('polyfill.dialog', registerDialog);
  }
}

async function resizePolyfill() {
  if (typeof window.ResizeObserver === 'undefined') {
    window.ResizeObserver = (await import('@juggle/resize-observer')).ResizeObserver;
  }
}
