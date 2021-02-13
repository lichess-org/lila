const trans = i18n => {
  var format = function (str, args) {
    if (args.length && str.includes('$s')) for (var i = 1; i < 4; i++) str = str.replace('%' + i + '$s', args[i - 1]);
    args.forEach(function (arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
  var list = function (str, args) {
    var segments = str.split(/(%(?:\d\$)?s)/g);
    for (var i = 1; i <= args.length; i++) {
      var pos = segments.indexOf('%' + i + '$s');
      if (pos !== -1) segments[pos] = args[i - 1];
    }
    for (var i = 0; i < args.length; i++) {
      var pos = segments.indexOf('%s');
      if (pos === -1) break;
      segments[pos] = args[i];
    }
    return segments;
  };

  const trans: Trans = function (key) {
    var str = i18n[key];
    return str ? format(str, Array.prototype.slice.call(arguments, 1)) : key;
  };
  trans.plural = function (key, count) {
    var pluralKey = key + ':' + lichess.quantity(count);
    var str = i18n[pluralKey] || i18n[key];
    return str ? format(str, Array.prototype.slice.call(arguments, 1)) : key;
  };
  trans.noarg = function (key) {
    // optimisation for translations without arguments
    return i18n[key] || key;
  };
  trans.vdom = function (key) {
    var str = i18n[key];
    return str ? list(str, Array.prototype.slice.call(arguments, 1)) : [key];
  };
  trans.vdomPlural = function (key, count) {
    var pluralKey = key + ':' + lichess.quantity(count);
    var str = i18n[pluralKey] || i18n[key];
    return str ? list(str, Array.prototype.slice.call(arguments, 2)) : [key];
  };
  return trans;
};

export default trans;
