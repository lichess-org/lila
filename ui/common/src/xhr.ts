export function json(url: string, init: RequestInit = {}): Promise<any> {
  return fetch(url, {
    headers: { 'Accept': 'application/vnd.lichess.v5+json' },
    cache: 'no-cache',
    credentials: 'same-origin',
    ...init
  }).then(res => {
    if (res.ok) return res.json();
    throw res.statusText;
  });
}

export function form(data: any) {
  const formData = new FormData();
  for (let k of data) formData.append(k, data[k]);
  return formData;
}
