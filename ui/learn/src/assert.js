var util = require('./util');

module.exports = {
  pieceNotOn: function(keys) {
    keys = util.readKeys(keys);
    return function(chess) {
      for (var key in chess.occupation())
        if (keys.indexOf(key) === -1) return true;
      return false;
    }
  }
};
