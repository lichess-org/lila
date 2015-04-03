var m = require('mithril');
var partial = require('chessground').util.partial;

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
}

function simulAction(action, ctrl) {
  ctrl.vm.loading = true;
  return m.request({
    method: 'POST',
    url: '/simul/' + ctrl.data.id + '/' + action,
    config: xhrConfig
  }).then(null, function() {
    // when the simul no longer exists
    location.reload();
  });
}

module.exports = {
  earlyStart: partial(simulAction, 'start'),
  join: partial(simulAction, 'join'),
  withdraw: partial(simulAction, 'withdraw'),
  accept: function(user) {
    simulAction('accept/' + user)
  },
  unaccept: function(user) {
    simulAction('unaccept/' + user)
  }
};
