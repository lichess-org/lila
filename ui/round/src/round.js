var mapValues = require('lodash-node/modern/objects/mapValues');

function isGamePlaying(data) {
  return data.game.started && !data.game.finished;
}

function isPlayerPlaying(data) {
  return isGamePlaying(data) && !data.player.spectator;
}

function parsePossibleMoves(possibleMoves) {
  return mapValues(possibleMoves, function(moves) {
    return moves.match(/.{2}/g);
  });
}

module.exports = {
  isGamePlaying: isGamePlaying,
  isPlayerPlaying: isPlayerPlaying,
  parsePossibleMoves: parsePossibleMoves
};
