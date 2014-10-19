var m = require('mithril');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
}

function reload(data) {
  data.reloading = true;
  m.redraw();
  var req = m.request({
    method: 'GET',
    url: data.url.round,
    config: xhrConfig
  });
  req.then(function() {
    data.reloading = false;
  });
  return req;
}

module.exports = {
  reload: reload
};
