function order(a, b) {
  return a.rating > b.rating ? -1 : 1;
}

function sort(ctrl) {
  ctrl.data.hooks.sort(order);
}

function fixBC(hook) {
  hook.mode = hook.mode === 'Casual' ? 0 : 1;
}

function init(hook) {
  hook.action = hook.uid === lichess.socket.settings.params.sri ? 'cancel' : 'join';
  fixBC(hook);
}

module.exports = {
  init: init,
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
