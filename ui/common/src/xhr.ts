export function json(url: string, init: RequestInit = {}): Promise<any> {
  return fetch(url, {
    headers: { Accept: "application/vnd.lishogi.v5+json" },
    cache: "no-cache",
    credentials: "same-origin",
    ...init,
  }).then((res) => {
    if (res.ok) return res.json();
    throw res.statusText;
  });
}

export function text(url: string, init: RequestInit = {}): Promise<any> {
  return fetch(url, {
    cache: "no-cache",
    credentials: "same-origin",
    ...init,
  }).then((res) => {
    if (res.ok) return res.text();
    throw res.statusText;
  });
}

export function form(data: any) {
  const formData = new FormData();
  for (const k of Object.keys(data)) formData.append(k, data[k]);
  return formData;
}
