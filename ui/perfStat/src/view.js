var m = require('mithril');

module.exports = function(ctrl) {
  var d = ctrl.data;
  return m('div', {
    config: lichess.powertip.manualGame
  }, [
    m('section.glicko', require('./glicko')(d)),
    m('section.counter', require('./counter')(d)),
    m('section.highlow', require('./highlow')(d)),
    m('section.resultStreak', require('./resultStreak')(d)),
    m('section.result', require('./result')(d)),
    m('section.playStreak', require('./playStreak').nb(d)),
    m('section.playStreak', require('./playStreak').time(d))
  ]);
};
