var m = require('mithril');
var ctrl = require('./ctrl');

module.exports = function(element, opts) {

  var controller = new ctrl(opts);

  m.module(element, {
    controller: function() {
      return controller;
    },
    view: require('./view')
  });

  return {
    update: controller.update,
    setVisible: controller.setVisible
  }
}
