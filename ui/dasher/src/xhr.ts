const headers = {
  'Accept': 'application/vnd.lichess.v2+json'
};

export function get(url: string, cache: boolean = false) {
  return $.ajax({
    url,
    headers,
    cache
  });
}
