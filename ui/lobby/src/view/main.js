var m = require('mithril');

var renderTabs = require('./tabs');
var renderRealTime = require('./realTime');

module.exports = function(ctrl) {
  var body;
  switch (ctrl.vm.tab) {
    case 'real_time':
      body = renderRealTime(ctrl);
      break;
  }
  return [
    m('div.tabs', renderTabs(ctrl)),
    body
  ];
};
