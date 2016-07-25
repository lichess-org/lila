var m = require('mithril');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
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
  }
};
