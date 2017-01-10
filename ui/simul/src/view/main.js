var m = require('mithril');

var created = require('./created');
var started = require('./started');
var finished = require('./finished');

module.exports = function(ctrl) {
  var handler;
  if (ctrl.data.isRunning) handler = started;
  else if (ctrl.data.isFinished) handler = finished;
  else handler = created;

  return m('div', {
      class: 'content_box no_padding simul_box simul_show'
    },
    handler(ctrl)
  );
};
