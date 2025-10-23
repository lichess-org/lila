const sw = self as unknown as ServiceWorkerGlobalScope;
const searchParams = new URL(sw.location.href).searchParams;
const assetBase = new URL(searchParams.get('asset-url')!, sw.location.href).href;
//let hasLocalCache = caches.has('local');

function assetUrl(path: string): string {
  return `${assetBase}assets/${path}`;
}

sw.addEventListener('install', () => sw.skipWaiting());

sw.addEventListener('activate', (e: ExtendableEvent) => {
  e.waitUntil(sw.clients.claim());
});

sw.addEventListener('push', (event: PushEvent) => {
  const data = event.data!.json();
  return event.waitUntil(
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

sw.addEventListener('notificationclick', (e: NotificationEvent) => e.waitUntil(handleNotificationClick(e)));

// experimental stuff below

// sw.addEventListener('message', async (e: ExtendableMessageEvent) => {
//   if (e.data && e.data.type !== 'cache') return;
//   if (e.data.value) {
//     const cache = await caches.open('local');
//     hasLocalCache = Promise.resolve(true);
//     await cacheLocalAssets(cache);
//   } else {
//     await caches.delete('local');
//     hasLocalCache = Promise.resolve(false);
//   }
// });

// sw.addEventListener('fetch', (e: FetchEvent) => {
//   if (e.request.method !== 'GET') return;
//   const path = new URL(e.request.url).pathname.match(
//     /^\/local(?:[/?#]?.*)?$|^\/assets\/lifat\/bots\/.+$|\/assets\/npm\/zerofish.+$/,
//   )?.[0];
//   if (!path) return;
//   e.respondWith(hasLocalCache.then(haz => (haz ? fetchLocalCache(e, path) : fetch(e.request))));
// });

// async function fetchLocalCache(e: FetchEvent, path: string): Promise<Response> {
//   const cache = await caches.open('local');

//   try {
//     if (!sw.navigator.onLine) throw new Response('offline', { status: 503 });
//     if (path.startsWith('/assets')) {
//       const rsp = await cache.match(e.request);
//       if (rsp) return rsp;
//     }
//     const netRsp = await fetch(e.request);
//     if (netRsp.status >= 300 && netRsp.status < 400) {
//       const redirectUrl = netRsp.headers.get('Location');
//       if (redirectUrl) return await fetch(redirectUrl);
//     }
//     //
//     if (!netRsp.ok) throw netRsp;

//     cache.put(e.request, netRsp.clone());
//     cacheLocalAssets(cache);

//     return netRsp;
//   } catch (err) {
//     console.log('serving cached content', err);

//     return (
//       (await caches.match(e.request)) ??
//       (err instanceof Response ? err : new Response('bad', { status: 500 }))
//     );
//   }
// }

// async function cacheLocalAssets(cache: Cache): Promise<void[]> {
//   const promises: Promise<void>[] = [];
//   const assetPaths: string[] = [];
//   const assets: Record<string, string[]> = await fetch('/bots/assets').then(res => res.json());

//   console.log('caching assets');
//   for (const [type, list] of Object.entries(assets)) {
//     for (const key of list) {
//       assetPaths.push(
//         ...(type === 'book'
//           ? [`data/bot/book/${key}.bin`, `lifat/bots/book/${key}.png`]
//           : [`data/bot/${type}/${key}`]),
//       );
//     }
//   }

//   for (const path of assetPaths) {
//     const assetRequest = new Request(assetUrl(path));
//     const cachedAsset = await cache.match(assetRequest);

//     if (cachedAsset) continue;
//     promises.push(
//       fetch(assetRequest).then(assetRsp => {
//         if (assetRsp.ok) {
//           return cache.put(assetRequest, assetRsp.clone());
//         }
//       }),
//     );
//   }

//   return Promise.all(promises);
// }
