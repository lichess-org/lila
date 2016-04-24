var m = require('mithril');
var router = require('game').router;

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
};

function uncache(url) {
  return url + '?_=' + new Date().getTime();
}

function reload(ctrl) {
  var req = m.request({
    method: 'GET',
    url: uncache(ctrl.data.url.round),
    config: xhrConfig
  });
  req.then(function() {}, function(err) {
    lichess.reload();
  });
  return req;
}

function whatsNext(ctrl) {
  return m.request({
    method: 'GET',
    url: uncache('/whats-next/' + ctrl.data.game.id + ctrl.data.player.id),
    config: xhrConfig
  });
}

function challengeRematch(gameId) {
  return m.request({
    method: 'POST',
    url: '/challenge/rematch-of/' + gameId,
    config: xhrConfig
  });
}

module.exports = {
  reload: reload,
  whatsNext: whatsNext,
  challengeRematch: challengeRematch
};
