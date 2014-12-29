function order(a, b) {
  return a.rating > b.rating ? -1 : 1;
}

function sort(ctrl) {
  ctrl.data.hooks.sort(order);
}

function init(hook) {
  hook.action = hook.uid === lichess.socket.settings.params.sri ? 'cancel' : 'join';
}

function initAll(ctrl) {
  ctrl.data.hooks.forEach(init);
  sort(ctrl);
}

module.exports = {
  init: init,
  initAll: initAll,
  sort: sort,
  add: function(ctrl, hook) {
    init(hook);
    ctrl.data.hooks.push(hook);
    sort(ctrl);
  },
  remove: function(ctrl, id) {
    ctrl.data.hooks = ctrl.data.hooks.filter(function(h) {
      return h.id !== id;
    });
    ctrl.vm.stepHooks.forEach(function(h) {
      if (h.id === id) h.disabled = true;
    });
  },
  syncIds: function(ctrl, ids) {
    ctrl.data.hooks = ctrl.data.hooks.filter(function(h) {
      return ids.indexOf(h.id) !== -1;
    });
  },
  find: function(ctrl, id) {
    return ctrl.data.hooks.filter(function(h) {
      return h.id === id;
    })[0];
  }
};
