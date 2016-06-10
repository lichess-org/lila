var m = require('mithril');

module.exports = function(opts) {

  var handlers = {
    timeout: opts.timeout
  };

  return {
    send: opts.send,
    receive: function(type, data) {
      console.log(type, data);
      console.log(handlers[type]);
      if (handlers[type]) {
        handlers[type](data);
        return true;
      }
      return false;
    }
  };
};
