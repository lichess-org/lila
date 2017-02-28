var m = require('mithril');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
}

function simulAction(action, ctrl) {
  return m.request({
    method: 'POST',
    url: '/simul/' + ctrl.data.id + '/' + action,
    config: xhrConfig
  }).then(null, function() {
    // when the simul no longer exists
    lichess.reload();
  });
}

module.exports = {
  start: lichess.partial(simulAction, 'start'),
  abort: lichess.partial(simulAction, 'abort'),
  join: function(variantKey) {
    return lichess.partial(simulAction, 'join/' + variantKey);
  },
  withdraw: lichess.partial(simulAction, 'withdraw'),
  accept: function(user) {
    return lichess.partial(simulAction, 'accept/' + user)
  },
  reject: function(user) {
    return lichess.partial(simulAction, 'reject/' + user)
  }
};
