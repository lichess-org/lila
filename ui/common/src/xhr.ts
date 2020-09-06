import { defined } from "./common";


const jsonHeader = {
  'Accept': 'application/vnd.lichess.v5+json'
};

export const xhrHeader = {
  'X-Requested-With': 'XMLHttpRequest' // so lila knows it's XHR
};

export const json = (url: string, init: RequestInit = {}): Promise<any> =>
  fetch(url, {
    headers: {
      ...jsonHeader,
      ...xhrHeader
    },
    cache: 'no-cache',
    credentials: 'same-origin',
    ...init
  })
    .then(res => {
      if (res.ok) return res.json();
      throw res.statusText;
    });


export const text = (url: string, init: RequestInit = {}): Promise<string> =>
  fetch(url, {
    headers: { ...xhrHeader },
    cache: 'no-cache',
    credentials: 'same-origin',
    ...init
  }).then(res => {
    if (res.ok) return res.text();
    throw res.statusText;
  });

export const script = (src: string): Promise<void> =>
  new Promise((resolve, reject) => {
    const nonce = document.body.getAttribute('data-nonce'),
    el = document.createElement('script');
    if (nonce) el.setAttribute('nonce', nonce);
    el.onload = resolve as () => void;
    el.onerror = reject;
    el.src = src;
    document.head.append(el);
  })

export const form = (data: any) => {
  const formData = new FormData();
  for (const k of Object.keys(data)) formData.append(k, data[k]);
  return formData;
}

export const url = (base: string, params: any) => {
  const searchParams = new URLSearchParams();
  for (const k of Object.keys(params)) if (defined(params[k])) searchParams.append(k, params[k]);
  return `${base}?${searchParams.toString()}`;
}

export const formToXhr = (el: HTMLFormElement) =>
  text(
    el.action, {
    method: el.method,
    body: new FormData(el)
  })
