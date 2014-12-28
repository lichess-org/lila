var m = require('mithril');

var renderTabs = require('./tabs');
var renderRealTimeList = require('./realTimeList');
var renderRealTimeChart = require('./realTimeChart');

module.exports = function(ctrl) {
  var body;
  switch (ctrl.vm.tab) {
    case 'real_time':
      switch (ctrl.vm.mode) {
        case 'chart':
          body = renderRealTimeChart(ctrl);
          break;
        default:
          body = renderRealTimeList(ctrl);
      }
  }
  return [
    m('div.tabs', renderTabs(ctrl)),
    body
  ];
};
