const headers = {
  'Accept': 'application/vnd.lichess.v3+json'
};

export function seeks() {
  return $.ajax({
    url: '/lobby/seeks',
    headers: headers
  });
}

export function nowPlaying() {
  return $.ajax({
    url: '/account/now-playing',
    headers: headers
  }).then(o => o.nowPlaying);
}

export function anonPoolSeek(pool) {
  return $.ajax({
    method: 'POST',
    url: '/setup/hook/' + window.lichess.StrongSocket.sri,
    data: {
      variant: 1,
      timeMode: 1,
      time: pool.lim,
      increment: pool.inc,
      days: 1,
      color: 'random'
    }
  });
}
