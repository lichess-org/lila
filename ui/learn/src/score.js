var util =  require('./util');

var apple = 50;
var capture = 50;

var levelBonus = {
  1: 500,
  2: 300,
  3: 100
};

function getLevelBonus(s, nbMoves) {
  var late = nbMoves - s.nbMoves;
  if (late <= 0) return levelBonus[1];
  if (late <= Math.max(1, s.nbMoves / 8)) return levelBonus[2];
  return levelBonus[3];
}

function getLevelMaxScore(l) {
  var score = util.readKeys(l.apples).length * apple;
  score += (l.captures || 0) * capture;
  return score + levelBonus[1];
}

function getLevelRank(l, score) {
  var max = getLevelMaxScore(l);
  if (score === max) return 1;
  if (score >= max - 200) return 2;
  return 3;
}

function getStageMaxScore(s) {
  return s.levels.reduce(function(sum, s) {
    return sum + getLevelMaxScore(s);
  }, 0);
}

function getStageRank(s, score) {
  var max = getStageMaxScore(s);
  if (typeof score !== 'number') score = score.reduce(function(a, b) { return a + b; }, 0);
  if (score >= max - Math.max(200, s.levels.length * 50)) return 1;
  if (score >= max - Math.max(200, s.levels.length * 300)) return 2;
  return 3;
}

module.exports = {
  apple: apple,
  capture: capture,
  getLevelRank: getLevelRank,
  getLevelBonus: getLevelBonus,
  getStageRank: getStageRank
};
