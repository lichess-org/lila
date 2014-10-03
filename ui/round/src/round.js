var mapValues = require('lodash-node/modern/objects/mapValues');
var status = require('./status');

function parsePossibleMoves(possibleMoves) {
  return mapValues(possibleMoves, function(moves) {
    return moves.match(/.{2}/g);
  });
}

function playable(data) {
  return status.started(data) && !status.finished(data);
}

function isPlayerPlaying(data) {
  return playable(data) && !data.player.spectator;
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
  if (data.player.color == color) return data.player;
  if (data.opponent.color == color) return data.opponent;
  return null;
}

function nbMoves(data, color) {
  return data.turns + (color == 'white' ? 1 : 0) / 2;
}

module.exports = {
  isPlayerPlaying: isPlayerPlaying,
  playable: playable,
  abortable: abortable,
  takebackable: takebackable,
  drawable: drawable,
  resignable: resignable,
  mandatory: mandatory,
  getPlayer: getPlayer,
  parsePossibleMoves: parsePossibleMoves
};
