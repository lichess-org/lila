var m = require('mithril');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
}

function reload(data) {
  return m.request({
    method: 'GET',
    url: data.url.round,
    config: xhrConfig
  });
}

module.exports = {
  reload: reload
};
