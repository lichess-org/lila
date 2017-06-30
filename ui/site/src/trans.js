lichess.trans = function(i18n) {
  var format = function(str, args) {
    if (args.length && str.indexOf('$s') > -1)
      for (var i = 1; i < 4; i++)
        str = str.replace('%' + i + '$s', args[i - 1]);
    args.forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };

  var trans = function(key) {
    var str = i18n[key];
    return str ? format(str, Array.prototype.slice.call(arguments, 1)) : key;
  };
  trans.plural = function(key, count) {
    var pluralKey = key + ':' + lichess.quantity(count);
    var str = i18n[pluralKey] || i18n[key];
    return str ? format(str, Array.prototype.slice.call(arguments, 1)) : key;
  };
  trans.noarg = function(key) {
    // optimisation for translations without arguments
    return i18n[key] || key;
  };
  trans.merge = function(more) {
    Object.keys(more).forEach(function(k) {
      i18n[k] = more[k];
    });
  };
  return trans;
};
