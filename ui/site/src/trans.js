lichess.trans = function(i18n) {
  var trans = function(key) {
    var str = i18n[key] || key;
    var args = Array.prototype.slice.call(arguments, 1);
    if (args.length && str.indexOf('$s') > -1) {
      for (var i = 1; i < 4; i++) {
        str = str.replace('%' + i + '$s', args[i - 1]);
      }
    }
    args.forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
  // optimisation for translations without arguments
  trans.noarg = function(key) {
    return i18n[key] || key;
  };
  trans.merge = function(more) {
    Object.keys(more).forEach(function(k) {
      i18n[k] = more[k];
    });
  };
  return trans;
};
