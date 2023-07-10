let redirectInProgress: false | string = false;

interface Opts {
  url: string;
  cookie: Cookie;
}

export const redirect = async (opts: string | Opts, notify?: boolean) => {
  if (notify)
    lichess.storage.boolean('pairingCountdown').getOrDefault(false)
      ? await lichess.sound.countdown(3)
      : await lichess.sound.play('genericNotify');
  let url: string;
  if (typeof opts == 'string') url = opts;
  else {
    url = opts.url;
    if (opts.cookie) {
      const cookie = [
        encodeURIComponent(opts.cookie.name) + '=' + opts.cookie.value,
        '; max-age=' + opts.cookie.maxAge,
        '; path=/',
        '; domain=' + location.hostname,
      ].join('');
      document.cookie = cookie;
    }
  }
  const href = '//' + location.host + '/' + url.replace(/^\//, '');
  redirectInProgress = href;
  location.href = href;
};

export const unload = {
  expected: false,
};

export const reload = () => {
  if (redirectInProgress) return;
  unload.expected = true;
  lichess.socket.disconnect();
  if (location.hash) location.reload();
  else location.assign(location.href);
};
