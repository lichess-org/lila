var m = require('mithril');

var renderTabs = require('./tabs');
var renderRealTime = require('./realTime/main');
var renderSeeks = require('./correspondence');

module.exports = function(ctrl) {
  var body;
  switch (ctrl.vm.tab) {
    case 'real_time':
      body = renderRealTime(ctrl);
      break;
    case 'seeks':
      body = renderSeeks(ctrl);
      break;
  }
  return [
    m('div.tabs', renderTabs(ctrl)),
    body
  ];
};
