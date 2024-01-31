const headers = {
  Accept: 'application/vnd.lishogi.v3+json',
};

export function seeks() {
  return $.ajax({
    url: '/lobby/seeks',
    headers: headers,
  });
}

export function nowPlaying() {
  return $.ajax({
    url: '/account/now-playing',
    headers: headers,
  }).then(o => o.nowPlaying);
}

