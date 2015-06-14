var m = require('mithril');
var partial = require('chessground').util.partial;
var throttle = require('./util').throttle;

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
}

// when the tournament no longer exists
function reloadPage() {
  location.reload();
}

function tourAction(action, ctrl) {
  return m.request({
    method: 'POST',
    url: '/tournament/' + ctrl.data.id + '/' + action,
    config: xhrConfig
  }).then(null, reloadPage);
}

function loadPage(ctrl, p) {
  return m.request({
    method: 'GET',
    url: '/tournament/' + ctrl.data.id + '/standing/' + p,
    config: xhrConfig
  }).then(ctrl.loadPage, reloadPage);
}

function reloadTournament(ctrl) {
  return m.request({
    method: 'GET',
    url: '/tournament/' + ctrl.data.id,
    config: xhrConfig,
    data: {
      page: ctrl.vm.page
    }
  }).then(ctrl.reload, reloadPage);
}

module.exports = {
  start: partial(tourAction, 'start'),
  join: partial(tourAction, 'join'),
  withdraw: partial(tourAction, 'withdraw'),
  loadPage: throttle(1000, false, loadPage),
  reloadTournament: throttle(1000, false, reloadTournament)
};
