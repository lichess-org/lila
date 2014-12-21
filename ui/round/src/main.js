var ctrl = require('./ctrl');
var view = require('./view/main');
var m = require('mithril');

module.exports = function(opts) {

  var controller = new ctrl(opts);

  m.module(opts.element, {
    controller: function () { return controller; },
    view: view
  });

  return {
    socketReceive: controller.socket.receive
  };
};
