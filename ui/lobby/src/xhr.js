var m = require('mithril');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
}

function seeks() {
  return m.request({
    method: 'GET',
    url: '/lobby/seeks',
    config: xhrConfig
  });
}

module.exports = {
  seeks: seeks
};
