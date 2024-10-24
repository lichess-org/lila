import { url as assetUrl, jsModule } from './asset';
import { log } from 'common/permalog';
import { storage } from 'common/storage';

export default async function () {
  if (!('serviceWorker' in navigator && 'Notification' in window && 'PushManager' in window)) return;
  const workerUrl = new URL(
    assetUrl(jsModule('serviceWorker'), { pathOnly: true, version: false }),
    self.location.href,
  );
  workerUrl.searchParams.set('asset-url', document.body.getAttribute('data-asset-url')!);
  const reg =
    (await navigator.serviceWorker.getRegistration().then(reg => reg?.update().then(() => reg))) ??
    (await navigator.serviceWorker.register(workerUrl.href, { scope: '/', updateViaCache: 'all' }));

  const store = storage.make('push-subscribed');
  const vapid = document.body.getAttribute('data-vapid');
  if (!vapid || Notification.permission !== 'granted') {
    store.remove();
    reg.pushManager.getSubscription().then(sub => sub?.unsubscribe());
    return;
  }
  const sub = await reg.pushManager.getSubscription();
  const resub = parseInt(store.get() || '0', 10) + 43200000 < Date.now(); // 12 hours
  const applicationServerKey = Uint8Array.from(atob(vapid), c => c.charCodeAt(0));

  if (sub && !resub) return;
  let newSub: PushSubscription | undefined = undefined;
  try {
    newSub = await reg.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: applicationServerKey,
    });
    if (!newSub) throw new Error(!!reg && JSON.stringify(await reg.pushManager.permissionState()));
    const res = await fetch('/push/subscribe', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newSub),
    });
    if (res.ok && !res.redirected) store.set('' + Date.now());
    else throw new Error(res.statusText);
  } catch (err: any) {
    log('push subscribe failed', err.message, newSub);
    newSub?.unsubscribe();
  }
}
