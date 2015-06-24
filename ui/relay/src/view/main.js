var m = require('mithril');
var util = require('chessground').util;
var games = require('./games');

module.exports = function(ctrl) {

  return m('div', {
    class: 'content_box no_padding relay_box relay_show'
  }, [
    m('h1.text[data-icon=n]', [
      ctrl.data.name,
      m('span.status', ctrl.data.status)
    ]),
    games(ctrl.data.games)
  ]);
};
