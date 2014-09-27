var chessground = require('chessground');
var chess = require('./chess');

function str2move(m) {
  return m ? [m.slice(0, 2), m.slice(2, 4), m[4]] : null;
}

module.exports = {
  str2move: str2move
};
