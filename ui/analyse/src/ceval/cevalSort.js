var defined = require('../util').defined;

var GT = -1;
var LT = 1;

var reverse = function(white) {
  return function(n) {
    return white ? n : -n;
  };
};

var sameSign = function(a, b) {
  return !(a >=0 ^ b >= 0);
};

module.exports = function(white) {
  var rev = reverse(white);
  return function(a, b) {
    var aMa = defined(a.mate) ? rev(a.mate) : undefined;
    var bMa = defined(b.mate) ? rev(b.mate) : undefined;
    if (defined(aMa)) {
      if (defined(bMa)) {
        if (sameSign(aMa, bMa)) return aMa < bMa ? GT : LT;
        return aMa < bMa ? LT : GT;
      }
      return aMa > 0 ? GT : LT;
    }
    if (defined(bMa)) return bMa > 0 ? LT : GT;
    return a.cp > b.cp;
  };
};
