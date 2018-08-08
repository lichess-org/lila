const headers = {
  'Accept': 'application/vnd.lidraughts.v2+json'
};

export function seeks() {
  return $.ajax({
    url: '/lobby/seeks',
    headers: headers
  });
}

export function nowPlaying() {
  return $.ajax({
    url: '/account/info',
    headers: headers
  }).then(function(o) {
    return o.nowPlaying;
  });
}

export function anonPoolSeek(pool) {
  return $.ajax({
    method: 'POST',
    url: '/setup/hook/' + window.lidraughts.StrongSocket.sri,
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
