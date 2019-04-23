// TODO: Check out https://developers.google.com/web/fundamentals/push-notifications/common-notification-patterns

self.addEventListener('push', event => {
  var data = event.data.json();
  event.waitUntil(self.registration.showNotification(data.title, {
    icon: 'https://lichess1.org/assets/images/logo.256.png',
    body: data.body,
    tag: data.stack,
    data: data.payload
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
    if (windowClients.length) windowClients[0].focus();
    else return clients.openWindow('/');
  }));
});
