var util =  require('./util');

var apple = 50;
var capture = 50;

function getLevelRank(s, nbMoves) {
  var late = nbMoves - s.nbMoves;
  if (late <= 0) return 1;
  else if (late <= Math.max(1, s.nbMoves / 8)) return 2;
  return 3;
}

var levelBonus = {
  1: 500,
  2: 300,
  3: 100
};

function getLevelBonus(rank) {
  return levelBonus[Math.min(rank, 3)];
}

function getLevelMaxScore(l) {
  return util.readKeys(l.apples).length * apple + levelBonus[1];
}

function getStageMaxScore(l) {
  return l.levels.reduce(function(sum, s) {
    return sum + getLevelMaxScore(s);
  }, 0);
}

function getStageRank(l, score) {
  var max = getStageMaxScore(l);
  if (score >= max - Math.max(200, l.levels.length * 50)) return 1;
  if (score >= max - Math.max(200, l.levels.length * 300)) return 2;
  return 3;
}

module.exports = {
  apple: apple,
  getLevelRank: getLevelRank,
  getLevelBonus: getLevelBonus,
  getStageRank: getStageRank
};
