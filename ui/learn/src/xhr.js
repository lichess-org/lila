var m = require('mithril');

function setScore(stageKey, score) {
  return m.request({
    method: 'POST',
    url: '/learn/stage',
    data: {
      stage: stageKey,
      score: score
    }
  });
}

module.exports = {
  setScore: setScore
};
