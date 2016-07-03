var util = require('../util');

var stages = [

  require('./rook'),
  require('./bishop'),
  require('./queen'),
  require('./king'),
  require('./knight'),
  require('./pawn'),

  require('./capture'),
  require('./check'),
  require('./checkmate'),
  require('./castling'),
  require('./enpassant'),

].map(util.toStage);

module.exports = {
  list: stages,
  get: function(id) {
    return stages.filter(function(l) {
      return l.id == id;
    })[0];
  }
};
