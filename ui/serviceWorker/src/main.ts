const searchParams = new URL(self.location.href).searchParams;
const isDev = searchParams.has('dev');
const assetBase = new URL(searchParams.get('asset-url')!, self.location.href).href;
const assetVersion = self.location.pathname.split('/')[2];

function assetUrl(path: string): string {
  const r = `${assetBase}assets/${assetVersion}/${path}`;
  console.log(r);
  return r;
}

function compiledScript(name: string): string {
  return `compiled/lichess.${name}${isDev ? '' : '.min'}.js`;
}

function themedStylesheets(name: string): string[] {
  return ['light', 'dark', 'transp'].map(theme => {
    return `css/${name}.${theme}.${isDev ? 'dev' : 'min'}.css`;
  });
}

self.addEventListener('install', event => {
  event.waitUntil(caches.open(assetVersion).then(cache => cache.addAll([
    ...[
      ...themedStylesheets('site'),
      'font/lichess.woff2',
      'font/lichess.chess.woff2',
    ].map(assetUrl),
    '/editor',
  ])));
});

self.addEventListener('fetch', event => {
  event.respondWith((async () => {
    const cache = await caches.open(assetVersion);
    const cachedReponse = await cache.match(event.request);
    if (cachedReponse) return cachedReponse;
    return fetch(event.request);
  })());
});

self.addEventListener('push', event => {
  const data = event.data!.json();
  return self.registration.showNotification(data.title, {
    badge: assetUrl('images/logo.256.png'),
    icon: assetUrl('images/logo.256.png'),
    body: data.body,
    tag: data.tag,
    data: data.payload,
    requireInteraction: true,
  });
});

async function handleNotificationClick(event: NotificationEvent) {
  const notifications = await self.registration.getNotifications();
  notifications.forEach(notification => notification.close());

  const windowClients = await self.clients.matchAll({
    type: 'window',
    includeUncontrolled: true,
  }) as ReadonlyArray<WindowClient>;

  // determine url
  const data = event.notification.data.userData;
  let url = '/';
  if (data.fullId) url = '/' + data.fullId;
  else if (data.threadId) url = '/inbox/' + data.threadId + '#bottom';
  else if (data.challengeId) url = '/' + data.challengeId;

  // focus open window with same url
  for (const client of windowClients) {
    const clientUrl = new URL(client.url, self.location.href);
    if (clientUrl.pathname === url && 'focus' in client) return await client.focus();
  }

  // navigate from open homepage to url
  for (const client of windowClients) {
    const clientUrl = new URL(client.url, self.location.href);
    if (clientUrl.pathname === '/') return await client.navigate(url);
  }

  // open new window
  return await self.clients.openWindow(url);
}

self.addEventListener('notificationclick', e => e.waitUntil(handleNotificationClick(e)));
