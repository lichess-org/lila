var title = require('./title');
var blur = require('./blur');
var round = require('./round');

module.exports = function(ctrl) {

  title.init(ctrl);
  ctrl.setTitle();

  blur.init(ctrl);

  if (round.playable(ctrl.data) && ctrl.data.game.turns < 2) $.sound.dong();
};
