var m = require('mithril');
var util = require('chessground').util;

var created = require('./created');
var started = require('./started');
var finished = require('./finished');

module.exports = function(ctrl) {
  var handler;
  if (ctrl.data.isStarted) handler = started;
  else if (ctrl.data.isFinished) handler = finished;
  else handler = created;

  var side = handler.side(ctrl);

  return [
    side ? m('div#tournament_side', side) : null,
    m('div', {
        class: util.classSet({
          'content_box no_padding tournament_box tournament_show': true,
          'finished': ctrl.data.isFinished
        })
      },
      handler.main(ctrl)
    )
  ];
};
