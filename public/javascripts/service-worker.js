self.addEventListener('push', event => {
  const data = event.data.json();
  event.waitUntil(self.registration.getNotifications().then(notifications => {
    notifications.forEach(notification => {
      if (notification.tag === data.stack) notification.close();
    });
    return self.registration.showNotification(data.title, {
      badge: 'https://lichess1.org/assets/images/logo.256.png',
      icon: 'https://lichess1.org/assets/images/logo.256.png',
      body: data.body,
      tag: data.stack,
      data: data.payload,
      requireInteraction: true
    });
  }));
});

self.addEventListener('notificationclick', event => {
  event.waitUntil(self.registration.getNotifications().then(notifications => {
    notifications.forEach(notification => notification.close());
    return clients.matchAll({
      type: 'window',
      includeUncontrolled: true
    });
  }).then(windowClients => {
    // determine url
    const data = event.notification.data.userData;
    let url = '/';
    if (data.fullId) url = '/' + data.fullId;
    else if (data.threadId) url = '/inbox/' + data.threadId + '#bottom';
    else if (data.challengeId) url = '/' + data.challengeId;

    // focus open window with same url
    for (let client of windowClients) {
      var clientUrl = new URL(client.url, self.location.href);
      if (clientUrl.pathname === url && 'focus' in client) return client.focus();
    }

    // navigate from open homepage to url
    for (let client of windowClients) {
      var clientUrl = new URL(client.url, self.location.href);
      if (clientUrl.pathname === '/') return client.navigate(url);
    }

    // open new window
    return clients.openWindow(url);
  }));
});
