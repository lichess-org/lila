export const json = (url: string, init: RequestInit = {}): Promise<any> => 
  fetch(url, {
    headers: { 'Accept': 'application/vnd.lichess.v5+json' },
    cache: 'no-cache',
    credentials: 'same-origin',
    ...init
  })
  .then(res => {
    if (res.ok) return res.json();
    throw res.statusText;
  });


export const text = (url: string, init: RequestInit = {}): Promise<any> => 
  fetch(url, {
    cache: 'no-cache',
    credentials: 'same-origin',
    ...init
  }).then(res => {
    if (res.ok) return res.text();
    throw res.statusText;
  });


export const form = (data: any) => {
  const formData = new FormData();
  for (const k of Object.keys(data)) formData.append(k, data[k]);
  return formData;
}
