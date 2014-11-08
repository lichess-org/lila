module.exports = function(old, cfg) {

  var data = cfg;

  if (cfg.game.moves) data.game.moves = data.game.moves.split(' ');

  return data;
};
