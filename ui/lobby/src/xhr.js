var m = require('mithril');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v2+json');
}

function uncache(url) {
  return url + '?_=' + new Date().getTime();
}

module.exports = {
  seeks: function() {
    return m.request({
      method: 'GET',
      url: uncache('/lobby/seeks'),
      config: xhrConfig,
      background: true
    });
  },
  nowPlaying: function() {
    return m.request({
      method: 'GET',
      url: uncache('/account/info'),
      config: xhrConfig
    }).then(function(o) {
      return o.nowPlaying;
    });
  },
  anonPoolSeek: function(pool) {
    return m.request({
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
