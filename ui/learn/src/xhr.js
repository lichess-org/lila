var m = require('mithril');

function setScore(levelKey, score) {
  return m.request({
    method: 'POST',
    url: '/learn/level',
    data: {
      level: levelKey,
      score: score
    }
  });
}

module.exports = {
  setScore: setScore
};
