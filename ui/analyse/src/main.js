var ctrl = require('./ctrl');
var view = require('./view');
var m = require('mithril');

module.exports = function(opts) {

  var controller = new ctrl(opts);

  m.module(opts.element, {
    controller: function () { return controller; },
    view: view
  });

  return {
    socketReceive: controller.socket.receive,
    jump: function(ply) {
      controller.jumpToMain(ply);
      m.redraw();
    },
    path: function() {
      return controller.vm.path;
    },
    pathStr: function() {
      return controller.vm.pathStr;
    }
  };
};

// lol, that's for the rest of lichess to access mithril
// without having to include it a second time
window.Chessground = require('chessground');
