var m = require('mithril');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
}

function uncache(url) {
  return url + '?_=' + new Date().getTime();
}

module.exports = {
  load: function(page) {
    return m.request({
      method: 'GET',
      url: uncache('/notify'),
      data: {
        page: page || 1
      },
      config: xhrConfig
    });
  }
};
