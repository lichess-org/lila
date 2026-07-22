import { defined, notNull } from './index';

export class ValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ValidationError';
  }
}

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

export const ensureOk = async (res: Response): Promise<Response> => {
  if (res.ok) return res;
  if (res.status === 413) throw new Error('The uploaded file is too large');
  if (res.status === 422) {
    const body = await res.json().catch(() => null);
    throw new ValidationError(body?.error || 'Unprocessable entity');
  }
  if (res.status === 429) throw new Error('Too many requests');
  throw new Error(`Error ${res.status} ${res.statusText} ${await res.text()}`);
};

/* fetch a static JSON asset without headers that trigger CORS preflight */
export const jsonSimple = (url: string, init: RequestInit = {}): Promise<any> =>
  fetch(url, {
    headers: {
      ...jsonHeader,
    },
    ...init,
  }).then(res => ensureOk(res).then(r => r.json()));

/* fetch a JSON value */
export const json = <A = any>(url: string, init: RequestInit = {}): Promise<A> =>
  jsonAnyResponse(url, init).then(res => ensureOk(res).then(r => r.json()));

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
  textRaw(url, init).then(res => ensureOk(res).then(r => r.text()));

export const textRaw = (url: string, init: RequestInit = {}): Promise<Response> =>
  fetch(url, {
    ...defaultInit,
    headers: { ...xhrHeader },
    ...init,
  });

/* load & inject a remote script */
export const script = (src: string): Promise<void> =>
  new Promise((resolve, reject) => {
    const nonce = document.body.getAttribute('data-nonce');
    const el = document.createElement('script');
    if (nonce) el.setAttribute('nonce', nonce);
    el.onload = () => resolve();
    el.onerror = reject;
    el.src = src;
    document.head.append(el);
  });

/* produce HTTP form data from a JS object */
export const form = (
  data: Record<string, string | string[] | boolean | number | null | undefined>,
): FormData => {
  const formData = new FormData();
  for (const k of Object.keys(data)) if (notNull(data[k])) formData.append(k, data[k].toString());
  return formData;
};

/* constructs a url with escaped parameters */
export const url = (path: string, params: Record<string, string | number | boolean | undefined>): string => {
  const searchParams = new URLSearchParams();
  for (const k of Object.keys(params)) if (defined(params[k])) searchParams.append(k, params[k].toString());
  const query = searchParams.toString();
  return query ? `${path}?${query}` : path;
};

/* submit a form with XHR */
export const formToXhr = (el: HTMLFormElement, submitter?: HTMLButtonElement): Promise<string> => {
  const action = el.getAttribute('action');
  const body = new FormData(el);
  if (submitter?.name && submitter?.value) body.set(submitter.name, submitter.value);
  return action
    ? text(action, {
        method: el.method,
        body,
      })
    : Promise.reject(new Error(`Form has no action: ${el}`));
};

export type ProcessLine<T> = (line: T) => void;

/*
 * `response` is the result of a `fetch` request.
 * `processLine` will be called with each element of the stream.
 * https://gist.github.com/ornicar/a097406810939cf7be1df8ea30e94f3e
 */
export const readNdJson = async <T>(response: Response, processLine: ProcessLine<T>): Promise<void> => {
  if (!response.ok) throw new Error(`Status ${response.status}`);
  const stream = response.body!.getReader();
  const matcher = /\r?\n/;
  const decoder = new TextDecoder();
  let buf = '';
  let done, value;
  do {
    ({ done, value } = await stream.read());
    buf += decoder.decode(value || new Uint8Array(), { stream: !done });
    const parts = buf.split(matcher);
    if (!done) buf = parts.pop()!;
    for (const part of parts) if (part) processLine(JSON.parse(part));
  } while (!done);
};

export async function writeTextClipboard(url: string, callbackOnSuccess?: () => void): Promise<void> {
  // Ancient browsers may not support `ClipboardItem`
  if (typeof ClipboardItem === 'undefined') {
    const t = await text(url);
    return navigator.clipboard.writeText(t).then(callbackOnSuccess);
  } else {
    const clipboardItem = new ClipboardItem({
      'text/plain': text(url).then(t => new Blob([t], { type: 'text/plain' })),
    });
    return navigator.clipboard.write([clipboardItem]).then(callbackOnSuccess);
  }
}
