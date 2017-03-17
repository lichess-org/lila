var headers = {
  'Accept': 'application/vnd.lichess.v2+json'
};

module.exports = {
  seeks: function() {
    return $.ajax({
      url: '/lobby/seeks',
      headers: headers
    });
  },
  nowPlaying: function() {
    return $.ajax({
      url: '/account/info',
      headers: headers
    }).then(function(o) {
      return o.nowPlaying;
    });
  },
  anonPoolSeek: function(pool) {
    return $.ajax({
      method: 'POST',
      url: '/setup/hook/' + lichess.StrongSocket.sri,
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
};
