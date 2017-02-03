var pv2san = require('ceval').pv2san;
var initialFen = require('chess').initialFen;

exports.testCrazyhouse = function(test) {
  var fen = 'r4b1N~/1ppk1P2/p1b5/6p1/8/1PBPPq2/P1PR1P2/1K4N1/PNBRPPPrqnn b - - 71 36';
  test.equal(pv2san('crazyhouse', fen, false, 'N@a3 b1b2 R@b1'.split(' '), -2), '36... N@a3+ 37. Kb2 R@b1#');
  test.done();
};

exports.testKingmove = function(test) {
  test.equal(pv2san('standard', initialFen, false, 'e2e4 e7e5 e1e2'.split(' ')), '1. e4 e5 2. Ke2');
  test.done();
};
