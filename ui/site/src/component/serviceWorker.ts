import { assetUrl, jsModule } from './assets';
import { storage } from './storage';

export default function () {
  if ('serviceWorker' in navigator && 'Notification' in window && 'PushManager' in window) {
    const workerUrl = new URL(
      assetUrl(jsModule('serviceWorker'), {
        sameDomain: true,
      }),
      self.location.href
    );
    workerUrl.searchParams.set('asset-url', document.body.getAttribute('data-asset-url')!);
    if (document.body.getAttribute('data-dev')) workerUrl.searchParams.set('dev', '1');
    const updateViaCache = document.body.getAttribute('data-dev') ? 'none' : 'all';
    navigator.serviceWorker
      .register(workerUrl.href, {
        scope: '/',
        updateViaCache,
      })
      .then(reg => {
        const store = storage.make('push-subscribed');
        const vapid = document.body.getAttribute('data-vapid');
        if (vapid && Notification.permission == 'granted')
          reg.pushManager.getSubscription().then(sub => {
            const resub = parseInt(store.get() || '0', 10) + 43200000 < Date.now(); // 12 hours
            const applicationServerKey = Uint8Array.from(atob(vapid), c => c.charCodeAt(0));
            if (!sub || resub) {
              reg.pushManager
                .subscribe({
                  userVisibleOnly: true,
                  applicationServerKey: applicationServerKey,
                })
                .then(
                  sub =>
                    fetch('/push/subscribe', {
                      method: 'POST',
                      headers: {
                        'Content-Type': 'application/json',
                      },
                      body: JSON.stringify(sub),
                    }).then(res => {
                      if (res.ok) store.set('' + Date.now());
                      else console.log('submitting push subscription failed', res.statusText);
                    }),
                  err => {
                    console.log('push subscribe failed', err.message);
                    if (sub) sub.unsubscribe();
                  }
                );
            }
          });
        else store.remove();
      });
  }
}
