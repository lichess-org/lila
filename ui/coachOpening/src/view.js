var m = require('mithril');

var piechart = require('./piechart');
var table = require('./table');
var inspect = require('./inspect');

module.exports = function(ctrl) {
  return [
    ctrl.vm.inspecting ? inspect(ctrl, ctrl.vm.inspecting) : m('div.top.chart', {
      config: function(el, isUpdate) {
        if (isUpdate) return;
        piechart(el, ctrl);
      }
    }),
    table(ctrl)
  ];
};
