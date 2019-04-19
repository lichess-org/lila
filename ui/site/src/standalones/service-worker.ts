console.log('hello service worker');

self.addEventListener('install', event => {
  console.log('install!');
});

self.addEventListener('push', event => {
  console.log('push received', event);
  event['waitUntil'](self.registration.showNotification('hello', {
    body: 'there'
  }));
});
