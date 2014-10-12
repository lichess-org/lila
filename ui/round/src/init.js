var title = require('./title');
var round = require('./round');

module.exports = function(ctrl) {

  title.init(ctrl);
  ctrl.setTitle();

  if (round.playable(ctrl.data) && ctrl.data.game.turns < 2) $.sound.dong();
};
