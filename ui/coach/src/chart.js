var m = require('mithril');

module.exports = function(ctrl) {
  if (ctrl.vm.loading) return m('div.square-spin');
  return m('div.chart', 'chart');
};
