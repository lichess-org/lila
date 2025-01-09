let redirectInProgress: string | undefined;

export function redirect(obj: string | { url: string; cookie: Cookie }): void {
  let url: string;
  if (typeof obj == 'string') url = obj;
  else {
    url = obj.url;
    if (obj.cookie) {
      const cookie = [
        encodeURIComponent(obj.cookie.name) + '=' + obj.cookie.value,
        '; max-age=' + obj.cookie.maxAge,
        '; path=/',
        '; domain=' + location.hostname,
      ].join('');
      document.cookie = cookie;
    }
  }
  const href = '//' + location.host + '/' + url.replace(/^\//, '');
  redirectInProgress = href;
  location.href = href;
}

export function reload(): void {
  if (redirectInProgress) return;
  window.lishogi.properReload = true;
  window.lishogi.socket.destroy(); // todo
  if (location.hash) location.reload();
  else location.href = location.href;
}
