var m = require('mithril');
var util = require('chessground').util;

var created = require('./created');
var started = require('./started');
var finished = require('./finished');

module.exports = function(ctrl) {
  var handler;
  if (ctrl.data.isRunning) handler = started;
  else if (ctrl.data.isFinished) handler = finished;
  else handler = created;

  return [
    m('div', {
      id: 'tournament_side',
      class: 'scroll-shadow-soft'
    }, handler.side(ctrl)),
    m('div', {
        class: 'content_box no_padding tournament_box tournament_show' + (ctrl.vm.loading ? ' loading' : '')
      },
      handler.main(ctrl)
    )
  ];
};
