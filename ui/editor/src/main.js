var ctrl = require('./ctrl');
var view = require('./view');

module.exports = function(element, config) {
  var controller = new ctrl(config);
  m.module(element, {
    controller: function () { return controller; },
    view: view
  });
};
