var status = require('./status');

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
  return !!data.tournament || !!data.simul;
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
    !data.simul &&
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

// can the current player go berserk?
function berserkableBy(data) {
  return data.tournament &&
    data.tournament.berserkable &&
    isPlayerPlaying(data) &&
    playedTurns(data) < 2;
}

function moretimeable(data) {
  return data.clock && data.moretimeable && isPlayerPlaying(data) && !mandatory(data);
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

function userAnalysable(data) {
  return playable(data) && (!data.clock || !isPlayerPlaying(data));
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
  moretimeable: moretimeable,
  mandatory: mandatory,
  replayable: replayable,
  userAnalysable: userAnalysable,
  getPlayer: getPlayer,
  nbMoves: nbMoves,
  setOnGame: setOnGame,
  setIsGone: setIsGone
};
