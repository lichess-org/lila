var m = require('mithril');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
};

function uncache(url) {
  return url + '?_=' + new Date().getTime();
}

module.exports = {

  reload: function(id, chapterId) {
    return m.request({
      method: 'GET',
      url: uncache('/study/' + id),
      data: {
        chapterId: chapterId
      },
      config: xhrConfig,
      background: true
    });
  },

  variants: function() {
    return m.request({
      method: 'GET',
      url: '/variant',
      config: xhrConfig,
      background: true
    });
  }
};
