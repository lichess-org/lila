var ctrl = require('./ctrl');
var view = require('./view');
var m = require('mithril');
var menuHover = require('common/menuHover').menuHover;

menuHover();

module.exports = function(element, config) {
  var controller = new ctrl(config);
  m.module(element, {
    controller: function () { return controller; },
    view: view
  });

  return {
    getFen: controller.computeFen,
    setOrientation: controller.setOrientation,
    changeVariant: controller.changeVariant
  };
};

// that's for the rest of lidraughts to access mithril
// without having to include it a second time
window.Draughtsground = require('draughtsground').Draughtsground;
