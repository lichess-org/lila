var player = function(data) {
  return '/' + data.game.id + data.player.id;
};
var game = function(data, color) {
  return '/' + data.game.id + (color ? '/' + color : '');
};

module.exports = {
  game: game,
  player: player,
  forecasts: function(data) {
    return player(data) + '/forecasts';
  }
};
