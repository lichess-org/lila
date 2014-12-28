var m = require('mithril');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
}

module.exports = {
  seeks: function() {
    return m.request({
      method: 'GET',
      url: '/lobby/seeks',
      config: xhrConfig
    });
  },
  nowPlaying: function() {
    return m.request({
      method: 'GET',
      url: '/lobby/playing',
      config: xhrConfig
    });
  }
};
