var m = require('mithril');

module.exports = function(ctrl) {
  var d = ctrl.data;
  return m('div', {
    config: lichess.powertip.manualGameIn
  }, [
    m('section.glicko', require('./glicko')(d)),
    m('section.counter.split', require('./counter')(d)),
    m('section.highlow.split', require('./highlow')(d)),
    m('section.resultStreak.split', require('./resultStreak')(d)),
    m('section.result.split', require('./result')(d)),
    m('section.playStreak', require('./playStreak').nb(d)),
    m('section.playStreak', require('./playStreak').time(d))
  ]);
};
