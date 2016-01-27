var m = require('mithril');
var xhr = require('./xhr');

module.exports = function(send, ctrl) {

  this.send = send;

  var handlers = {
  };

  this.receive = function(type, data) {
    if (handlers[type]) {
      handlers[type](data);
      return true;
    }
    return false;
  }.bind(this);
};
