var m = require('mithril');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
}

function uncache(url) {
  return url + '?_=' + new Date().getTime();
}

function reload(ctrl) {
  var req = m.request({
    method: 'GET',
    url: uncache(ctrl.data.url.round),
    config: xhrConfig
  });
  req.then(function() {
  }, function(err) {
    lichess.reload();
  });
  return req;
}

function berserk(ctrl) {
  return m.request({
    method: 'POST',
    url: '/tournament/' + ctrl.data.game.tournamentId + '/berserk',
    config: xhrConfig
  });
}

function whatsNext(ctrl) {
  return m.request({
    method: 'GET',
    url: uncache(ctrl.router.Round.whatsNext(ctrl.data.game.id).url),
    config: xhrConfig
  });
}

module.exports = {
  reload: reload,
  berserk: berserk,
  whatsNext: whatsNext
};
