var m = require('mithril');

var list = require('./list');
var chart = require('./chart');
var filter = require('../../filter');
var filterView = require('./filter');

module.exports = function(ctrl) {
  var filterBody, body, nbFiltered, modeToggle;
  if (ctrl.vm.filterOpen) filterBody = filterView.render(ctrl);
  switch (ctrl.vm.mode) {
    case 'chart':
      var res = filter(ctrl, ctrl.data.hooks),
        hooks = res.visible;
      nbFiltered = res.hidden;
      body = filterBody || chart.render(ctrl, hooks);
      modeToggle = ctrl.vm.filterOpen ? null : chart.toggle(ctrl);
      break;
    default:
      var res = filter(ctrl, ctrl.vm.stepHooks),
        hooks = res.visible;
      nbFiltered = res.hidden;
      body = filterBody || list.render(ctrl, hooks);
      modeToggle = ctrl.vm.filterOpen ? null : list.toggle(ctrl);
  }
  var filterToggle = filterView.toggle(ctrl, nbFiltered);
  return [
    filterToggle,
    modeToggle,
    body
  ];
}
