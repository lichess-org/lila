var m = require('mithril');
var util = require('chessground').util;

var created = require('./created');
var started = require('./started');

module.exports = function(ctrl) {
  var handler;
  if (ctrl.data.isRunning) handler = started;
  else if (ctrl.data.isFinished) handler = null;
  else handler = created;

  return [
    m('div', {
      class: ctrl.vm.loading ? 'loading' : ''
    }, m('div', {
        class: 'content_box no_padding tournament_box tournament_show'
      },
      handler.main(ctrl)
    )),
    m('div', {
      id: 'tournament_side',
      class: 'scroll-shadow-soft'
    }, handler.side(ctrl))
  ];
};
