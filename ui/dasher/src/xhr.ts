const headers = {
  'Accept': 'application/vnd.lichess.v2+json'
};

export function load() {
  return $.ajax({
    url: '/dasher',
    headers: headers
  })
}
