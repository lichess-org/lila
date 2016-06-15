var ctrl = require('./ctrl');
var view = require('./view/main');
var m = require('mithril');

module.exports = function(opts) {

  var controller = new ctrl(opts);

  m.module(opts.element, {
    controller: function() {
      return controller;
    },
    view: view
  });

  return {
    socketReceive: controller.socket.receive,
    moveOn: controller.moveOn
  };
};

// lol, that's for the rest of lichess to access mithril
// without having to include it a second time
window.Chessground = require('chessground');
