const sw = self as ServiceWorkerGlobalScope & typeof globalThis;
// https://github.com/microsoft/TypeScript/issues/14877

const searchParams = new URL(sw.location.href).searchParams;
const assetBase = new URL(searchParams.get('asset-url')!, sw.location.href).href;

sw.addEventListener('install', () => {
  sw.skipWaiting();
});

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

function assetUrl(path: string): string {
  return `${assetBase}assets/${path}`;
}

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

sw.addEventListener('fetch', e => {
  const path = new URL(e.request.url).pathname.match(/(\/local$|\/local\?.*)/)?.[1];
  if (!path) return;
  e.respondWith(resolveFetch(path, e));
});

async function resolveFetch(path: string, e: FetchEvent): Promise<Response> {
  try {
    const rsp = await fetch(e.request);
    if (!rsp.ok) throw rsp;
    await caches.open('local').then(cache => cache.put(path, rsp.clone()));
    return rsp;
  } catch (rsp) {
    return (
      (await caches.match(path)) ?? (rsp instanceof Response ? rsp : new Response('bad', { status: 500 }))
    );
  }
}
