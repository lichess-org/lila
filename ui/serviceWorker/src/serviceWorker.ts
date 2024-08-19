const sw = self as ServiceWorkerGlobalScope & typeof globalThis;
// https://github.com/microsoft/TypeScript/issues/14877

const searchParams = new URL(sw.location.href).searchParams;
const assetBase = new URL(searchParams.get('asset-url')!, sw.location.href).href;
let hasLocalCache = caches.has('local');

function assetUrl(path: string): string {
  return `${assetBase}assets/${path}`;
}

sw.addEventListener('install', () => sw.skipWaiting());

sw.addEventListener('activate', e => {
  e.waitUntil(clients.claim());
});

sw.addEventListener('push', e => {
  const data = e.data!.json();
  return e.waitUntil(
    sw.registration.showNotification(data.title, {
      badge: assetUrl('logo/lichess-mono-128.png'),
      icon: assetUrl('logo/lichess-favicon-192.png'),
      body: data.body,
      tag: data.tag,
      data: data.payload,
      requireInteraction: true,
    }),
  );
});

async function handleNotificationClick(e: NotificationEvent) {
  const notifications = await sw.registration.getNotifications();
  notifications.forEach(notification => notification.close());

  const windowClients = (await sw.clients.matchAll({
    type: 'window',
    includeUncontrolled: true,
  })) as ReadonlyArray<WindowClient>;

  // determine url
  const data = e.notification.data.userData;
  let url = data.path || '/';
  if (data.fullId) url = '/' + data.fullId;
  else if (data.threadId) url = '/inbox/' + data.threadId;
  else if (data.challengeId) url = '/' + data.challengeId;
  else if (data.streamerId) url = `/streamer/${data.streamerId}/redirect`;
  else if (data.mentionedBy) url = `/forum/redirect/post/${data.postId}`;
  else if (data.invitedBy) url = `/study/${data.studyId}`;

  // focus open window with same url
  for (const client of windowClients) {
    const clientUrl = new URL(client.url, sw.location.href);
    if (clientUrl.pathname === url && 'focus' in client) return await client.focus();
  }

  // navigate from open homepage to url
  for (const client of windowClients) {
    const clientUrl = new URL(client.url, sw.location.href);
    if (clientUrl.pathname === '/') return await client.navigate(url);
  }

  // open new window
  return await sw.clients.openWindow(url);
}

sw.addEventListener('notificationclick', e => e.waitUntil(handleNotificationClick(e)));

sw.addEventListener('message', e => {
  if (e.data && e.data.type !== 'cache') return;
  e.waitUntil(
    (async () => {
      if (e.data.value) {
        const cache = await caches.open('local');
        hasLocalCache = Promise.resolve(true);
        await cacheLocalAssets(cache);
      } else {
        await caches.delete('local');
        hasLocalCache = Promise.resolve(false);
      }
    })(),
  );
});

// experimental stuff below

sw.addEventListener('fetch', e => {
  const path = new URL(e.request.url).pathname.match(
    /^\/local(?:[/?#]?.*)?$|^\/assets\/lifat\/bots\/.+$|\/assets\/npm\/zerofish.+$/,
  )?.[0];
  if (!path) return;
  e.respondWith(hasLocalCache.then(haz => (haz ? fetchLocalCache(e, path) : fetch(e.request))));
});

async function fetchLocalCache(e: FetchEvent, path: string): Promise<Response> {
  const cache = await caches.open('local');

  try {
    if (!sw.navigator.onLine) throw new Response('offline', { status: 503 });
    if (path.startsWith('/assets')) {
      const rsp = await cache.match(e.request);
      if (rsp) return rsp;
    }
    const rsp = await fetch(e.request);
    if (!rsp.ok) throw rsp;

    cache.put(e.request, rsp.clone());
    cacheLocalAssets(cache);

    return rsp;
  } catch (err) {
    console.log('serving cached content', err);

    return (
      (await caches.match(e.request)) ??
      (err instanceof Response ? err : new Response('bad', { status: 500 }))
    );
  }
}

async function cacheLocalAssets(cache: Cache): Promise<void[]> {
  const promises: Promise<void>[] = [];
  const assetPaths: string[] = [];
  const assets: Record<string, string[]> = await fetch('/local/assets').then(res => res.json());

  console.log('caching assets');
  for (const [type, list] of Object.entries(assets)) {
    for (const key of list) {
      assetPaths.push(
        ...(type === 'book'
          ? [`lifat/bots/books/${key}.bin`, `lifat/bots/books/${key}.png`]
          : [`lifat/bots/${type}s/${key}`]),
      );
    }
  }

  for (const path of assetPaths) {
    const assetRequest = new Request(assetUrl(path));
    const cachedAsset = await cache.match(assetRequest);

    if (cachedAsset) continue;
    promises.push(
      fetch(assetRequest).then(assetRsp => {
        if (assetRsp.ok) {
          return cache.put(assetRequest, assetRsp.clone());
        }
      }),
    );
  }

  return Promise.all(promises);
}
