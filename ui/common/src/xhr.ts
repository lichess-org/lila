import { defined } from './common';

const jsonHeader = {
  Accept: 'application/vnd.lichess.v5+json',
};

export const defaultInit: RequestInit = {
  cache: 'no-cache',
  credentials: 'same-origin', // required for safari < 12
};

export const xhrHeader = {
  'X-Requested-With': 'XMLHttpRequest', // so lila knows it's XHR
};

/* fetch a JSON value */
export const json = (url: string, init: RequestInit = {}): Promise<any> =>
  fetch(url, {
    ...defaultInit,
    headers: {
      ...jsonHeader,
      ...xhrHeader,
    },
    ...init,
  }).then(res => {
    if (res.ok) return res.json();
    throw res.statusText;
  });

/* fetch a string */
export const text = (url: string, init: RequestInit = {}): Promise<string> =>
  textRaw(url, init).then(res => {
    if (res.ok) return res.text();
    throw res.statusText;
  });

export const textRaw = (url: string, init: RequestInit = {}): Promise<Response> =>
  fetch(url, {
    ...defaultInit,
    headers: { ...xhrHeader },
    ...init,
  });

/* load a remote script */
export const script = (src: string): Promise<void> =>
  new Promise((resolve, reject) => {
    const nonce = document.body.getAttribute('data-nonce'),
      el = document.createElement('script');
    if (nonce) el.setAttribute('nonce', nonce);
    el.onload = resolve as () => void;
    el.onerror = reject;
    el.src = src;
    document.head.append(el);
  });

/* produce HTTP form data from a JS object */
export const form = (data: any) => {
  const formData = new FormData();
  for (const k of Object.keys(data)) formData.append(k, data[k]);
  return formData;
};

/* constructs a url with escaped parameters */
export const url = (path: string, params: { [k: string]: string | number | boolean | undefined }) => {
  const searchParams = new URLSearchParams();
  for (const k of Object.keys(params)) if (defined(params[k])) searchParams.append(k, params[k] as string);
  const query = searchParams.toString();
  return query ? `${path}?${query}` : path;
};

/* submit a form with XHR */
export const formToXhr = (el: HTMLFormElement): Promise<string> => {
  const action = el.getAttribute('action');
  return action
    ? text(action, {
        method: el.method,
        body: new FormData(el),
      })
    : Promise.reject(`Form has no action: ${el}`);
};
