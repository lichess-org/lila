var m = require('mithril');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
}

function showLoading(ctrl) {
  ctrl.vm.loading = true;
  m.redraw();
}

function uncache(url) {
  return url + '?_=' + new Date().getTime();
}

module.exports = {
  attempt: function(ctrl) {
    showLoading(ctrl);
    m.request({
      method: 'POST',
      url: ctrl.data.opening.url,
      data: {
        found: ctrl.vm.figuredOut.length,
        failed: ctrl.vm.messedUp.length
      },
      config: xhrConfig
    }).then(ctrl.reload);
  },
  newOpening: function(ctrl) {
    showLoading(ctrl);
    m.request({
      method: 'GET',
      url: uncache('/training/opening'),
      config: xhrConfig
    }).then(ctrl.reload);
  }
};
