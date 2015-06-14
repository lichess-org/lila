var m = require('mithril');
var partial = require('chessground').util.partial;

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
}

// when the tournament no longer exists
function reloadPage() {
  location.reload();
}

function tourAction(action, ctrl) {
  ctrl.vm.loading = true;
  return m.request({
    method: 'POST',
    url: '/tournament/' + ctrl.data.id + '/' + action,
    config: xhrConfig
  }).then(null, reloadPage);
}

function loadPage(ctrl, p) {
  ctrl.vm.loading = true;
  return m.request({
    method: 'GET',
    url: '/tournament/' + ctrl.data.id + '/standing/' + p,
    config: xhrConfig
  }).then(ctrl.loadPage, reloadPage);
}

module.exports = {
  start: partial(tourAction, 'start'),
  join: partial(tourAction, 'join'),
  withdraw: partial(tourAction, 'withdraw'),
  loadPage: loadPage
};
