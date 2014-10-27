var ctrl = require('./ctrl');
var view = require('./view');

module.exports = function(element, config, router, i18n) {

  var controller = new ctrl(config, router, i18n);

  m.module(element, {
    controller: function () { return controller; },
    view: view
  });
};
