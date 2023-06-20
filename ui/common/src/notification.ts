let notifications: Array<Notification> = [];
let listening = false;

function listenToFocus() {
  if (!listening) {
    listening = true;
    window.addEventListener('focus', () => {
      notifications.forEach(n => n.close());
      notifications = [];
    });
  }
}

function notify(msg: string | (() => string)) {
  const storage = lichess.storage.make('just-notified');
  if (document.hasFocus() || Date.now() - parseInt(storage.get()!, 10) < 1000) return;
  storage.set('' + Date.now());
  if ($.isFunction(msg)) msg = msg();
  const notification = new Notification('lichess.org', {
    icon: lichess.assetUrl('logo/lichess-favicon-256.png', { noVersion: true }),
    body: msg,
  });
  notification.onclick = () => window.focus();
  notifications.push(notification);
  listenToFocus();
}

export default function (msg: string | (() => string)) {
  if (document.hasFocus() || !('Notification' in window)) return;
  if (Notification.permission === 'granted') {
    // increase chances that the first tab can put a local storage lock
    setTimeout(notify, 10 + Math.random() * 500, msg);
  }
}
