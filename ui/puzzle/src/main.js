var ctrl = require('./ctrl');
var view = require('./view/main');
var sideView = require('./view/side');
var m = require('mithril');

module.exports = function(opts) {

  var controller = new ctrl(opts);

  m.module(opts.element, {
    controller: function() {
      return controller;
    },
    view: view
  });

  m.module(opts.sideElement, {
    controller: function() {
      m.redraw.strategy("diff"); // prevents double full redraw on page load
      return controller;
    },
    view: sideView
  });

  return {
    socketReceive: controller.socketReceive
  };
};

// lol, that's for the rest of lichess to access mithril
// without having to include it a second time
window.Chessground = require('chessground');
