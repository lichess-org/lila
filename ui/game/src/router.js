var player = function(data) {
  return '/' + data.game.id + data.player.id;
};
var game = function(data, color, embed) {
  return (embed ? '/embed/' : '/') + (data.game ? data.game.id : data) + (color ? '/' + color : '');
};

module.exports = {
  game: game,
  player: player,
  forecasts: function(data) {
    return player(data) + '/forecasts';
  },
  continue: function(data, mode) {
    return game(data) + '/continue/' + mode;
  }
};
