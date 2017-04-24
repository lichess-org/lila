var headers = {
  'Accept': 'application/vnd.lichess.v2+json'
};

function reload(ctrl) {
  return $.ajax({
    url: ctrl.data.url.round,
    headers: headers
  }).fail(lichess.reload);
}

function whatsNext(ctrl) {
  return $.ajax({
    url: '/whats-next/' + ctrl.data.game.id + ctrl.data.player.id,
    headers: headers
  });
}

function challengeRematch(gameId) {
  return $.ajax({
    method: 'POST',
    url: '/challenge/rematch-of/' + gameId,
    headers: headers
  });
}

module.exports = {
  reload: reload,
  whatsNext: whatsNext,
  challengeRematch: challengeRematch
};
