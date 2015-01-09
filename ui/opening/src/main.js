var m = require('mithril');
var ctrl = require('./ctrl');
var view = require('./view');

module.exports = function(element, config, router, i18n) {
  var controller = new ctrl(config, router, i18n);
  m.module(element, {
    controller: function () { return controller; },
    view: view
  });
  controller.pushState(config);
};

// lol, that's for the rest of lichess to access mithril
// without having to include it a second time
window.Chessground = require('chessground');
