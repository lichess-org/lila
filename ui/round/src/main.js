var ctrl = require('./ctrl');
var view = require('./view/main');
var m = require('mithril');

module.exports = function(element, config, router, i18n, socketSend) {

  var controller = new ctrl(config, router, i18n, socketSend);

  m.module(element, {
    controller: function () { return controller; },
    view: view
  });

  return {
    socketReceive: controller.socket.receive
  };
};
