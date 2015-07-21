var m = require('mithril');

var highchart = require('./highchart');
var table = require('./table');

module.exports = function(ctrl) {
  return [
    m('div.chart', {
      config: function(el, isUpdate) {
        if (isUpdate) return;
        highchart(el, ctrl.data);
      }
    }),
    table(ctrl)
  ];
};
