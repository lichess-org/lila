var m = require('mithril');

function completeLesson(gameId) {
  return m.request({
    method: 'POST',
    url: '/challenge/rematch-of/' + gameId,
    config: xhrConfig
  });
}

module.exports = {
  reload: reload,
  whatsNext: whatsNext,
  challengeRematch: challengeRematch
};

