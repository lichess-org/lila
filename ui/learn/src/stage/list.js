var util = require('../util');

var stages = [

  require('./rook'),
  require('./bishop'),
  require('./queen'),
  require('./king'),
  require('./knight'),
  require('./pawn'),

  require('./capture'),
  require('./check1'),
  require('./outOfCheck.js'),

  require('./checkmate1'),
  require('./stalemate'),
  require('./value'),

  require('./setup'),
  require('./castling'),
  require('./enpassant'),

  require('./check2'),

].map(util.toStage);

var stagesByKey = {}
stages.forEach(function(s) {
  stagesByKey[s.key] = s;
});

var stagesById = {}
stages.forEach(function(s) {
  stagesById[s.id] = s;
});

var categs = [{
  name: 'Chess pieces',
  stages: [
    'rook', 'bishop', 'queen', 'king', 'knight', 'pawn'
  ]
}, {
  name: 'Fundamentals',
  stages: [
    'capture', 'check1', 'outOfCheck', 'checkmate1'
  ]
}, {
  name: 'Intermediate',
  stages: [
    'setup', 'castling', 'enpassant', 'stalemate'
  ]
}, {
  name: 'Advanced',
  stages: [
    'check2', /*'checkmate2, '*/ 'value'
  ]
}].map(function(c) {
  c.stages = c.stages.map(function(key) {
    return stagesByKey[key];
  });
  return c;
});

module.exports = {
  list: stages,
  byId: stagesById,
  categs: categs,
  stageIdToCategId: function(stageId) {
    var stage = stagesById[stageId];
    for (var id in categs)
      if (categs[id].stages.some(function(s) {
        return s.key === stage.key;
      })) return id;
  }
};
