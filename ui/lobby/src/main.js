var m = require('mithril');

var ctrl = require('./ctrl');
var view = require('./view/main');
var legacy = require('./legacy');

module.exports = {
  mithril: function(element, opts) {

    opts.element = element;

    var controller = new ctrl(opts);

    m.module(element, {
      controller: function() {
        return controller;
      },
      view: view
    });

    return {
      socketReceive: controller.socket.receive,
      setTab: function(tab) {
        controller.setTab(tab);
        m.redraw();
      },
      gameActivity: controller.gameActivity,
      setRedirecting: controller.setRedirecting,
      enterPool: controller.enterPool,
      leavePool: controller.leavePool
    };
  },
  legacy: legacy
};

// lol, that's for the rest of lichess to access mithril
// without having to include it a second time
window.Chessground = require('chessground');
