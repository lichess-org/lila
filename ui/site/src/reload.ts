import { promiseTimeout } from 'common/promise';

let redirectInProgress: false | string = false;

interface Opts {
  url: string;
  cookie: Cookie;
}

export const redirect = async (opts: string | Opts, beep?: boolean) => {
  try {
    if (beep) await promiseTimeout(site.sound.play('genericNotify'), 1000);
  } catch (e) {
    console.warn(e);
  }
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

export const reload = (err?: any) => {
  if (err) console.warn(err);
  if (redirectInProgress) return;
  unload.expected = true;
  site.socket.disconnect();
  if (location.hash) location.reload();
  else location.assign(location.href);
};
