var m = require('mithril');
var partial = require('chessground').util.partial;
var throttle = require('./util').throttle;

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
}

function uncache(url) {
  return url + '?_=' + new Date().getTime();
}

// when the tournament no longer exists
function reloadPage() {
  lichess.reload();
}

function tourAction(action, ctrl) {
  return m.request({
    method: 'POST',
    url: '/tournament/' + ctrl.data.id + '/' + action,
    config: xhrConfig
  }).then(null, reloadPage);
}

function loadPage(ctrl, p) {
  m.request({
    method: 'GET',
    url: uncache('/tournament/' + ctrl.data.id + '/standing/' + p),
    config: xhrConfig
  }).then(ctrl.loadPage, reloadPage);
}

function reloadTournament(ctrl) {
  return m.request({
    method: 'GET',
    url: uncache('/tournament/' + ctrl.data.id),
    config: xhrConfig,
    data: {
      page: ctrl.vm.focusOnMe ? null : ctrl.vm.page,
      playerInfo: ctrl.vm.playerInfo.id
    }
  }).then(ctrl.reload, reloadPage);
}

function playerInfo(ctrl, userId) {
  return m.request({
    method: 'GET',
    url: uncache(['/tournament', ctrl.data.id, 'player', userId].join('/')),
    config: xhrConfig
  }).then(ctrl.setPlayerInfoData, reloadPage);
}

module.exports = {
  join: throttle(1000, false, partial(tourAction, 'join')),
  withdraw: throttle(1000, false, partial(tourAction, 'withdraw')),
  loadPage: throttle(1000, false, loadPage),
  reloadTournament: throttle(2000, false, reloadTournament),
  playerInfo: playerInfo
};
