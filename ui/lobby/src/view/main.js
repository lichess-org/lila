var m = require('mithril');

var renderTabs = require('./tabs');
var renderPools = require('./pools');
var renderRealTime = require('./realTime/main');
var renderSeeks = require('./correspondence');
var renderPlaying = require('./playing');

module.exports = function(ctrl) {
  var body;
  if (ctrl.playban || ctrl.currentGame) return;
  switch (ctrl.vm.tab) {
    case 'pools':
      body = renderPools.render(ctrl);
      break;
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
    m('div.lobby_box.' + ctrl.vm.tab, body),
    (ctrl.vm.tab === 'pools' && !ctrl.data.me) ? renderPools.anonOverlay() : null
  ];
};
