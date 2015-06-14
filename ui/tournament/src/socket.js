var m = require('mithril');
var xhr = require('./xhr');

module.exports = function(send, ctrl) {

  this.send = send;

  var handlers = {
    reload: function(data) {
      // require to restart the clock
      if (ctrl.data.isStarted !== data.isStarted) m.redraw.strategy('all');
      ctrl.reload(data);
      m.redraw();
    },
    deleted: function() {
      lichess.reload();
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
