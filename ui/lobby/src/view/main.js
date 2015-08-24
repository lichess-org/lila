var m = require('mithril');

var renderTabs = require('./tabs');
var renderRealTime = require('./realTime/main');
var renderSeeks = require('./correspondence');
var renderPlaying = require('./playing');

module.exports = function(ctrl) {
  var body;
  if (ctrl.playban || ctrl.currentGame) return;
  switch (ctrl.vm.tab) {
    case 'real_time':
      body = renderRealTime(ctrl);
      break;
    case 'seeks':
      body = renderSeeks(ctrl);
      break;
    case 'now_playing':
      body = renderPlaying(ctrl);
      break;
  }
  return [
    m('div.tabs', renderTabs(ctrl)),
    m('div.lobby_box', body)
  ];
};
