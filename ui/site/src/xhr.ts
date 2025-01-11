const defaultInit: RequestInit = {
  cache: 'no-cache',
  credentials: 'same-origin',
};
const xhrHeader = {
  'X-Requested-With': 'XMLHttpRequest',
};
const jsonHeader = {
  Accept: 'application/vnd.lishogi.v5+json',
};

type FetchMethod = 'POST' | 'GET' | 'DELETE';
const fetchWrap = (
  method: FetchMethod,
  url: string,
  content: LishogiFetchContent | undefined,
  init: RequestInit,
): Promise<Response> => {
  const fullUrl = content?.url ? urlWithParams(url, content.url) : url;
  const body = content?.json
    ? JSON.stringify(content.json)
    : content?.formData
      ? formData(content.formData)
      : undefined;

  return fetch(fullUrl, {
    ...defaultInit,
    ...init,
    headers: {
      ...(init.headers || {}),
      ...(content?.json ? { 'Content-Type': 'application/json' } : {}),
    },
    method,
    body: body,
  }).then(res => {
    if (res.ok) return res;
    throw res.statusText;
  });
};

export const json = <T = any>(
  method: FetchMethod,
  url: string,
  content?: LishogiFetchContent,
  init?: LishogiFetchInit,
): Promise<T> => {
  return fetchWrap(method, url, content, {
    headers: { ...xhrHeader, ...jsonHeader },
    ...init,
  }).then(res => res.json());
};

export const text = (
  method: FetchMethod,
  url: string,
  content?: LishogiFetchContent,
  init?: LishogiFetchInit,
): Promise<any> => {
  return fetchWrap(method, url, content, {
    headers: { ...xhrHeader },
    ...init,
  }).then(res => res.text());
};

export const formToXhr = (
  el: HTMLFormElement,
  submitter?: HTMLButtonElement,
): Promise<Response> => {
  const action = el.getAttribute('action');
  const method = (el.method || 'GET') as 'GET' | 'POST';
  const body = new FormData(el);
  if (submitter?.name && submitter?.value) {
    body.set(submitter.name, submitter.value);
  }
  return action
    ? fetchWrap(method, action, undefined, {
        headers: { ...xhrHeader },
        body,
      })
    : Promise.reject(`Form has no action: ${el}`);
};

export const urlWithParams = (
  base: string,
  params: Record<string, string | boolean | undefined | null | number>,
): string => {
  const searchParams = new URLSearchParams();
  for (const k of Object.keys(params)) {
    let value = params[k];
    if (value !== undefined && value !== null) {
      if (typeof value === 'boolean') value = value ? '1' : '0';
      searchParams.append(k, value as string);
    }
  }
  const queryString = searchParams.toString();
  return queryString ? `${base}?${queryString}` : base;
};

const formData = (data: Record<string, string | number | undefined | Blob>): FormData => {
  const formData = new FormData();
  for (const k of Object.keys(data)) {
    if (data[k] !== undefined) formData.append(k, data[k] as string);
  }
  return formData;
};
