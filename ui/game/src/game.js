var mapValues = require('lodash-node/modern/objects/mapValues');
var status = require('./status');

function parsePossibleMoves(possibleMoves) {
  return mapValues(possibleMoves, function(moves) {
    return moves.match(/.{2}/g);
  });
}

function playable(data) {
  return data.game.status.id < status.ids.aborted;
}

function isPlayerPlaying(data) {
  return playable(data) && !data.player.spectator;
}

function isPlayerTurn(data) {
  return isPlayerPlaying(data) && data.game.player == data.player.color;
}

function mandatory(data) {
  return !!data.tournament;
}

function playedTurns(data) {
  return data.game.turns - data.game.startedAtTurn;
}

function abortable(data) {
  return playable(data) && playedTurns(data) < 2 && !mandatory(data);
}

function takebackable(data) {
  return playable(data) &&
    data.takebackable &&
    !data.tournament &&
    playedTurns(data) > 1 &&
    !data.player.proposingTakeback &&
    !data.opponent.proposingTakeback;
}

function drawable(data) {
  return playable(data) &&
    data.game.turns >= 2 &&
    !data.player.offeringDraw &&
    !data.opponent.ai;
}

function resignable(data) {
  return playable(data) && !abortable(data);
}

function berserkOf(data, color) {
  return data.tournament && data.tournament[color === 'white' ? 'berserk1' : 'berserk2'];
}

// can the current player go berserk?
function berserkableBy(data) {
  return data.tournament &&
    data.tournament.berserkable &&
    isPlayerPlaying(data) &&
    playedTurns(data) < 2 &&
    !berserkOf(data, data.player.color);
}

function moretimeable(data) {
  return data.clock && isPlayerPlaying(data) && !mandatory(data);
}

function replayable(data) {
  return data.source == 'import' || status.finished(data);
}

function getPlayer(data, color) {
  if (data.player.color == color) return data.player;
  if (data.opponent.color == color) return data.opponent;
  return null;
}

function hasAi(data) {
  return data.player.ai || data.opponent.ai;
}

var userAnalysableVariants = [
  'standard',
  'fromPosition',
  'chess960',
  'kingOfTheHill'
];

function userAnalysable(data) {
  return playable(data) &&
    (!data.clock || !isPlayerPlaying(data)) &&
    userAnalysableVariants.indexOf(data.game.variant.key) !== -1;
}

function setOnGame(data, color, onGame) {
  var player = getPlayer(data, color);
  onGame = onGame || player.ai;
  player.onGame = onGame;
  if (onGame) setIsGone(data, color, false);
}

function setIsGone(data, color, isGone) {
  var player = getPlayer(data, color);
  isGone = isGone && !player.ai;
  player.isGone = isGone;
  if (!isGone && player.user) player.user.online = true;
}

function nbMoves(data, color) {
  return Math.floor((data.game.turns + (color == 'white' ? 1 : 0)) / 2);
}

module.exports = {
  isPlayerPlaying: isPlayerPlaying,
  isPlayerTurn: isPlayerTurn,
  playable: playable,
  abortable: abortable,
  takebackable: takebackable,
  drawable: drawable,
  resignable: resignable,
  berserkableBy: berserkableBy,
  berserkOf: berserkOf,
  moretimeable: moretimeable,
  mandatory: mandatory,
  replayable: replayable,
  userAnalysable: userAnalysable,
  userAnalysableVariants: userAnalysableVariants,
  getPlayer: getPlayer,
  parsePossibleMoves: parsePossibleMoves,
  nbMoves: nbMoves,
  setOnGame: setOnGame,
  setIsGone: setIsGone
};
