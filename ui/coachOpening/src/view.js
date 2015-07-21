var m = require('mithril');

var highchart = require('./highchart');
var table = require('./table');
var inspect = require('./inspect');

module.exports = function(ctrl) {
  return [
    ctrl.vm.inspecting ? inspect(ctrl, ctrl.vm.inspecting) : m('div.top.chart', {
      config: function(el, isUpdate) {
        if (isUpdate) return;
        highchart(el, ctrl);
      }
    }),
    table(ctrl)
  ];
};
