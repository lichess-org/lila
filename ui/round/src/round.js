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

function playable(data) {
  return data.game.started && !data.game.finished;
}

function mandatory(data) {
  return data.tournamentId || data.poolId;
}

function abortable(data) {
  return playable(data) && data.game.turns < 2 && !mandatory(data);
}

function takebackable(data) {
  return playable(data) && data.takebackable && !data.tournamentId && data.game.turns > 1 && !data.player.isProposingTakeback && !data.opponent.isProposingTakeback;
}

function drawable(data) {
  return playable(data) && data.game.turns >= 2 && !data.player.isOfferingDraw && !data.opponent.ai;
}

function resignable(data) {
  return playable(data) && !abortable(data);
}

function getPlayer(data, color) {
  return data.player.color == color ? data.player : data.opponent;
}

module.exports = {
  isGamePlaying: isGamePlaying,
  isPlayerPlaying: isPlayerPlaying,
  playable: playable,
  abortable: abortable,
  takebackable: takebackable,
  drawable: drawable,
  resignable: resignable,
  getPlayer: getPlayer,
  parsePossibleMoves: parsePossibleMoves
};
