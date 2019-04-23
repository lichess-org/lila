self.addEventListener('push', event => {
  var data = event.data.json();
  event.waitUntil(self.registration.getNotifications().then(notifications => {
    notifications.forEach(notification => {
      if (notification.tag === data.stack) notification.close();
    });
    return self.registration.showNotification(data.title, {
      badge: 'https://lichess1.org/assets/images/logo.256.png',
      icon: 'https://lichess1.org/assets/images/logo.256.png',
      body: data.body,
      tag: data.stack,
      data: data.payload
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
    var data = event.notification.data.userData;
    if (data.fullId) return clients.openWindow('/' + data.fullId);
    if (data.threadId) return clients.openWindow('/inbox/' + data.threadId + '#bottom');
    if (data.challengeId) return clients.openWindow('/' + data.challengeId);
    if (windowClients.length) windowClients[0].focus();
    else return clients.openWindow('/');
  }));
});
