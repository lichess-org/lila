var ctrl = require('./ctrl');
var view = require('./view/main');
var boot = require('./boot');
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

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = require('chessground').Chessground;

window.onload = function() {
  if (window.lichess_round) boot(window.lichess_round, document.getElementById('lichess'));
};
