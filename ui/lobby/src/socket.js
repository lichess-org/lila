var m = require('mithril');
var xhr = require('./xhr');
var hookRepo = require('./hookRepo');

module.exports = function(send, ctrl) {

  this.send = send;

  var handlers = {
    hook_add: function(hook) {
      hookRepo.add(ctrl, hook);
      // if (hook.action == 'cancel') $('body').trigger('lichess.hook-flush');
      m.redraw();
    },
    hook_remove: function(id) {
      hookRepo.remove(ctrl, id);
      m.redraw();
    },
    hook_list: function(ids) {
      hookRepo.syncIds(ctrl, ids);
      m.redraw();
    }
  };

  this.receive = function(type, data) {
    // if (type != 'n') console.log(type, data);
    if (handlers[type]) {
      handlers[type](data);
      return true;
    }
    return false;
  }.bind(this);
};
