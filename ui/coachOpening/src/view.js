var m = require('mithril');

var highchart = require('./highchart');

function colorChart(data, color) {
  return m('div.' + color, {
    config: function(el, isUpdate) {
      if (isUpdate) return;
      highchart(el, data, color);
    }
  });
}

module.exports = function(ctrl) {
  return m('div.charts ', ['white'].map(function(color) {
    return colorChart(ctrl.data, color);
  }));
};
