var ctrl = require('./ctrl');
var view = require('./view');
var m = require('mithril');

module.exports = function(element, config, router, i18n, onChange) {

  var controller = new ctrl(config, router, i18n, onChange);

  m.module(element, {
    controller: function () { return controller; },
    view: view
  });

  return {
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
