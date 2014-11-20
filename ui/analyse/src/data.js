module.exports = function(old, cfg) {

  var data = cfg;

  if (cfg.game.moves) data.game.moves = data.game.moves.split(' ');
  else data.game.moves = [];

  return data;
};
