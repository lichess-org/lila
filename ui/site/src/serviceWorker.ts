import { url as assetUrl, jsModule } from './asset';
import { storage } from './storage';

export default async function () {
  if (!('serviceWorker' in navigator && 'Notification' in window && 'PushManager' in window)) return;
  const workerUrl = new URL(
    assetUrl(jsModule('serviceWorker'), { pathOnly: true }),
    self.location.href, // eslint-disable-line no-restricted-globals
  );
  workerUrl.searchParams.set('asset-url', document.body.getAttribute('data-asset-url')!);
  const reg = await navigator.serviceWorker.register(workerUrl.href, {
    scope: '/',
    updateViaCache: 'all',
  });
  const store = storage.make('push-subscribed');
  const vapid = document.body.getAttribute('data-vapid');
  if (vapid && Notification.permission == 'granted') {
    const sub = await reg.pushManager.getSubscription();
    const resub = parseInt(store.get() || '0', 10) + 43200000 < Date.now(); // 12 hours
    const applicationServerKey = Uint8Array.from(atob(vapid), c => c.charCodeAt(0));
    if (!sub || resub) {
      const newSub = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: applicationServerKey,
      });
      try {
        const res = await fetch('/push/subscribe', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(newSub),
        });
        if (res.ok && !res.redirected) store.set('' + Date.now());
        else newSub.unsubscribe();
      } catch (err: any) {
        console.log('push subscribe failed', err.message);
        newSub?.unsubscribe();
      }
    }
  } else {
    store.remove();
    (await reg.pushManager.getSubscription())?.unsubscribe();
  }
}
