var m = require('mithril');

var ctrl = require('./ctrl');
var view = require('./view/main');

module.exports = function(element, opts) {

  var controller = new ctrl(opts);

  m.module(element, {
    controller: function () { return controller; },
    view: view
  });

  return {
    socketReceive: controller.socket.receive
  };
};

// lol, that's for the rest of lichess to access mithril
// without having to include it a second time
window.Chessground = require('chessground');
