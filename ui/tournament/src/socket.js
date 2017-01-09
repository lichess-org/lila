var xhr = require('./xhr');

module.exports = function(send, ctrl) {

  var handlers = {
    reload: function() {
      xhr.reloadTournament(ctrl);
    },
    redirect: function(fullId) {
      ctrl.redirectFirst(fullId.slice(0, 8), true);
      return true;
    }
  };

  return {
    send: send,
    receive: function(type, data) {
      if (handlers[type]) return handlers[type](data);
      return false;
    }
  };
};
