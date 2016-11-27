var ctrl = require('./ctrl');
var view = require('./view');
var m = require('mithril');

module.exports = function(element, config, i18n) {
  var controller = new ctrl(config, i18n);
  m.module(element, {
    controller: function () { return controller; },
    view: view
  });
  controller.initiate();
  controller.pushState(config);
};

// lol, that's for the rest of lichess to access mithril
// without having to include it a second time
window.Chessground = require('chessground');
