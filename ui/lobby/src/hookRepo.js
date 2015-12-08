function ratingOrder(a, b) {
  return (a.rating || 0) > (b.rating || 0) ? -1 : 1;
}

function timeOrder(a, b) {
  return a.t < b.t ? -1 : 1;
}

function sort(ctrl, hooks) {
  hooks.sort(ctrl.vm.sort === 'time' ? timeOrder : ratingOrder);
}

function init(hook) {
  hook.action = hook.uid === lichess.socket.settings.params.sri ? 'cancel' : 'join';
  hook.variant = hook.variant || 'standard';
}

function initAll(ctrl) {
  ctrl.data.hooks.forEach(init);
}

module.exports = {
  init: init,
  initAll: initAll,
  sort: sort,
  add: function(ctrl, hook) {
    init(hook);
    ctrl.data.hooks.push(hook);
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
