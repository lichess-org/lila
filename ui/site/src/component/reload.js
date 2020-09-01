lichess.hasToReload = false;
lichess.redirectInProgress = false;
lichess.redirect = obj => {
  let url;
  if (typeof obj == "string") url = obj;
  else {
    url = obj.url;
    if (obj.cookie) {
      const domain = document.domain.replace(/^.+(\.[^.]+\.[^.]+)$/, '$1');
      const cookie = [
        encodeURIComponent(obj.cookie.name) + '=' + obj.cookie.value,
        '; max-age=' + obj.cookie.maxAge,
        '; path=/',
        '; domain=' + domain
      ].join('');
      document.cookie = cookie;
    }
  }
  const href = '//' + location.host + '/' + url.replace(/^\//, '');
  lichess.redirectInProgress = href;
  location.href = href;
};
lichess.reload = () => {
  if (lichess.redirectInProgress) return;
  lichess.hasToReload = true;
  lichess.socket?.disconnect();
  if (location.hash) location.reload();
  else location.href = location.href;
};
