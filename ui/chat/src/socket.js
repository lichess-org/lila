var m = require('mithril');

module.exports = function(send) {

  var handlers = {};

  return {
    send: send,
    receive: function(type, data) {
      if (handlers[type]) {
        handlers[type](data);
        return true;
      }
      return false;
    }
  };
};
