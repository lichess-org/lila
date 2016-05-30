var m = require('mithril');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
}

function uncache(url) {
  return url + '?_=' + new Date().getTime();
}

module.exports = {
    load: function() {
        return m.request({
            method: 'GET',
            url: uncache('/notif'),
            config: xhrConfig

        });
    },
    markAllRead: function() {
        return m.request({
            method: 'POST',
            url: '/notif',
            config: xhrConfig

        });
    }
};
