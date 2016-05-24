var ctrl = require('./ctrl');
var view = require('./view');
var m = require('mithril');

module.exports = function(element, config) {
  var controller = new ctrl(config);
  m.module(element, {
    controller: function () { return controller; },
    view: view
  });

  return {
    getFen: controller.computeFen,
    setOrientation: controller.setOrientation
  };
};
