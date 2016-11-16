var ctrl = require('./ctrl');
var view = require('./view/main');
var m = require('mithril');

module.exports = function(opts) {

  var controller = new ctrl(opts);

  m.mount(opts.element, {
    oninit: function(vnode) {
      vnode.state = controller;
    },
    view: view
  });

  return {
    socketReceive: controller.socket.receive,
    moveOn: controller.moveOn
  };
};

// lol, that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = require('chessground');
