import { defined } from './common';

export const jsonHeader = {
  Accept: 'application/web.lichess+json',
};

export const defaultInit: RequestInit = {
  cache: 'no-cache',
  credentials: 'same-origin', // required for safari < 12
};

export const xhrHeader = {
  'X-Requested-With': 'XMLHttpRequest', // so lila knows it's XHR
};

export const ensureOk = (res: Response): Response => {
  if (res.ok) return res;
  if (res.status == 429) throw new Error('Too many requests');
  if (res.status == 413) throw new Error('The uploaded file is too large');
  throw new Error(`Error ${res.status}`);
};

/* fetch a static JSON asset without headers that trigger CORS preflight */
export const jsonSimple = (url: string, init: RequestInit = {}): Promise<any> =>
  fetch(url, {
    headers: {
      ...jsonHeader,
    },
    ...init,
  }).then(res => ensureOk(res).json());

/* fetch a JSON value */
export const json = (url: string, init: RequestInit = {}): Promise<any> =>
  jsonAnyResponse(url, init).then(res => ensureOk(res).json());

export const jsonAnyResponse = (url: string, init: RequestInit = {}): Promise<any> =>
  fetch(url, {
    ...defaultInit,
    headers: {
      ...jsonHeader,
      ...xhrHeader,
    },
    ...init,
  });

/* fetch a string */
export const text = (url: string, init: RequestInit = {}): Promise<string> =>
  textRaw(url, init).then(res => ensureOk(res).text());

export const textRaw = (url: string, init: RequestInit = {}): Promise<Response> =>
  fetch(url, {
    ...defaultInit,
    headers: { ...xhrHeader },
    ...init,
  });

/* load & inject a remote script */
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
export const form = (data: any): FormData => {
  const formData = new FormData();
  for (const k of Object.keys(data)) if (defined(data[k])) formData.append(k, data[k]);
  return formData;
};

/* constructs a url with escaped parameters */
export const url = (path: string, params: { [k: string]: string | number | boolean | undefined }): string => {
  const searchParams = new URLSearchParams();
  for (const k of Object.keys(params)) if (defined(params[k])) searchParams.append(k, params[k] as string);
  const query = searchParams.toString();
  return query ? `${path}?${query}` : path;
};

/* submit a form with XHR */
export const formToXhr = (el: HTMLFormElement, submitter?: HTMLButtonElement): Promise<string> => {
  const action = el.getAttribute('action');
  const body = new FormData(el);
  if (submitter?.name && submitter?.value) {
    body.set(submitter.name, submitter.value);
  }
  return action
    ? text(action, {
        method: el.method,
        body,
      })
    : Promise.reject(`Form has no action: ${el}`);
};
