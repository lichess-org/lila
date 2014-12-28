var m = require('mithril');
var xhr = require('./xhr');
var hookRepo = require('./hookRepo');

module.exports = function(send, ctrl) {

  this.send = send;

  var handlers = {
    hook_add: function(hook) {
      hookRepo.add(ctrl, hook);
      if (hook.action === 'cancel') ctrl.flushHooks();
      if (ctrl.vm.tab === 'real_time') m.redraw();
    },
    hook_remove: function(id) {
      hookRepo.remove(ctrl, id);
      if (ctrl.vm.tab === 'real_time') m.redraw();
    },
    hook_list: function(ids) {
      hookRepo.syncIds(ctrl, ids);
      if (ctrl.vm.tab === 'real_time') m.redraw();
    },
    reload_seeks: function() {
      if (ctrl.vm.tab === 'seeks') xhr.seeks().then(ctrl.setSeeks);
    }
  };

  this.receive = function(type, data) {
    if (handlers[type]) {
      handlers[type](data);
      return true;
    }
    return false;
  }.bind(this);
};
