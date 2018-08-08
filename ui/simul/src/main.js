var m = require('mithril');

var ctrl = require('./ctrl');
var view = require('./view/main');

module.exports = function(element, opts) {

  var controller = new ctrl(opts);

  m.module(element, {
    controller: function() {
      return controller;
    },
    view: view
  });

  return {
    socketReceive: controller.socket.receive
  };
};

window.lidraughtsChat = require('chat');
