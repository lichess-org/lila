var ctrl = require('./ctrl');
var view = require('./view');

module.exports = function(element, config, router, i18n, onChange) {

  var controller = new ctrl(config, router, i18n, onChange);

  m.module(element, {
    controller: function () { return controller; },
    view: view
  });

  return controller;
};
