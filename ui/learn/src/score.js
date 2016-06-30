var util =  require('./util');

var apple = 50;

function getStageRank(s, nbMoves) {
  var late = nbMoves - s.nbMoves;
  if (late <= 0) return 1;
  else if (late <= Math.max(1, s.nbMoves / 8)) return 2;
  return 3;
}

var stageBonus = {
  1: 500,
  2: 300,
  3: 100
};

function getStageBonus(rank) {
  return stageBonus[Math.min(rank, 3)];
}

function getStageMaxScore(l) {
  return util.readKeys(l.apples).length * apple + stageBonus[1];
}

function getLevelMaxScore(l) {
  return l.stages.reduce(function(sum, s) {
    return sum + getStageMaxScore(s);
  }, 0);
}

function getLevelRank(l, score) {
  var max = getLevelMaxScore(l);
  if (score >= max - Math.max(200, l.stages.length * 50)) return 1;
  if (score >= max - Math.max(200, l.stages.length * 300)) return 2;
  return 3;
}

module.exports = {
  apple: apple,
  getStageRank: getStageRank,
  getStageBonus: getStageBonus,
  getLevelRank: getLevelRank
};
