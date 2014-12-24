var m = require('mithril');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
}

function reload(ctrl) {
  ctrl.vm.reloading = true;
  m.redraw();
  var req = m.request({
    method: 'GET',
    url: ctrl.data.url.round,
    config: xhrConfig
  });
  req.then(function() {
    ctrl.vm.reloading = false;
  }, function(err) {
    lichess.reload();
  });
  return req;
}

function next(ctrl) {
  return m.request({
    method: 'GET',
    url: ctrl.router.Round.next(ctrl.data.game.id).url,
    config: xhrConfig
  });
}

module.exports = {
  reload: reload,
  next: next
};
