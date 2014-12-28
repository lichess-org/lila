function order(a, b) {
  return a.rating > b.rating ? -1 : 1;
}

function sort(ctrl) {
  ctrl.data.hooks.sort(order);
}

module.exports = {
  sort: sort,
  add: function(ctrl, hook) {
    hook.action = hook.uid === lichess.socket.settings.params.sri ? 'cancel' : 'join';
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
  stepSlice: function(ctrl) {
    return ctrl.data.hooks.slice(0, 14);
  },
  find: function(ctrl, id) {
    return ctrl.data.hooks.filter(function(h) {
      return h.id === id;
    })[0];
  }
};
