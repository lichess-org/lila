var status = require('./status');

function playable(data) {
  return data.game.status.id < status.ids.aborted && !imported(data);
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

function bothPlayersHavePlayed(data) {
  return playedTurns(data) > 1;
}

function abortable(data) {
  return playable(data) && !bothPlayersHavePlayed(data) && !mandatory(data);
}

function takebackable(data) {
  return playable(data) &&
    data.takebackable &&
    !data.tournament &&
    !data.simul &&
    bothPlayersHavePlayed(data) &&
    !data.player.proposingTakeback &&
    !data.opponent.proposingTakeback;
}

function drawable(data) {
  return playable(data) &&
    data.game.turns >= 2 &&
    !data.player.offeringDraw &&
    !hasAi(data);
}

function resignable(data) {
  return playable(data) && !abortable(data);
}

// can the current player go berserk?
function berserkableBy(data) {
  return data.tournament &&
    data.tournament.berserkable &&
    isPlayerPlaying(data) &&
    !bothPlayersHavePlayed(data);
}

function moretimeable(data) {
  return data.clock && isPlayerPlaying(data) && !mandatory(data);
}

function imported(data) {
  return data.game.source === 'import';
}

function replayable(data) {
  return imported(data) || status.finished(data) ||
    (status.aborted(data) && bothPlayersHavePlayed(data));
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

function isCorrespondence(data) {
  return data.game.speed === 'correspondence';
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
  setIsGone: setIsGone,
  isCorrespondence: isCorrespondence,
  isSwitchable: function(data) {
    return !hasAi(data) && (data.simul || isCorrespondence(data));
  }
};
