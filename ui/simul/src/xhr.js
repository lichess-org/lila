var m = require('mithril');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
}

function partial() {
  return arguments[0].bind.apply(arguments[0], [null].concat(Array.prototype.slice.call(arguments, 1)));
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
  start: partial(simulAction, 'start'),
  abort: partial(simulAction, 'abort'),
  join: function(variantKey) {
    return partial(simulAction, 'join/' + variantKey);
  },
  withdraw: partial(simulAction, 'withdraw'),
  accept: function(user) {
    return partial(simulAction, 'accept/' + user)
  },
  reject: function(user) {
    return partial(simulAction, 'reject/' + user)
  },
  setText: function(ctrl, text) {
    return m.request({
      method: 'POST',
      url: '/simul/' + ctrl.data.id + '/set-text',
      config: xhrConfig,
      data: {
        text: text
      }
    });
  }
};
