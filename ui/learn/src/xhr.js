var m = require('mithril');

function saveScore(stageKey, levelId, score) {
  return m.request({
    method: 'POST',
    url: '/learn/score',
    data: {
      stage: stageKey,
      level: levelId,
      score: score
    }
  });
}

module.exports = {
  saveScore: saveScore
};
