lichess.trans = function(i18n) {
  var lang = document.documentElement.lang;

  var quantity = function(c) {
    switch (lang) {
      case 'fr': case 'ff': case 'kab':
        // french
        return (c < 2) ? 'one' : 'other';
      case 'cs': case 'sk':
        // czech
        if (c == 1) return 'one';
        else if (c >= 2 && c <= 4) return 'few';
        else return 'other';
      case 'hr': case 'ru': case 'sr': case 'uk': case 'be': case 'bs': case 'sh':
        // balkan
        var rem100 = c % 100;
        var rem10 = c % 10;
        if (rem10 == 1 && rem100 != 11) return 'one';
        else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) return 'few';
        else if (rem10 == 0 || (rem10 >= 5 && rem10 <= 9) || (rem100 >= 11 && rem100 <= 14)) return 'many';
        else return 'other';
      case 'lv':
        // latvian
        if (c == 0) return 'zero';
        else if (c % 10 == 1 && c % 100 != 11) return 'one';
        else return 'other';
      case 'lt':
        // lithuanian
        var rem100 = c % 100;
        var rem10 = c % 10;
        if (rem10 == 1 && !(rem100 >= 11 && rem100 <= 19)) return 'one';
        else if (rem10 >= 2 && rem10 <= 9 && !(rem100 >= 11 && rem100 <= 19)) return 'few';
        else return 'other';
      case 'pl':
        // polish
        var rem100 = c % 100;
        var rem10 = c % 10;
        if (c == 1) return 'one';
        else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) return 'few';
        else return 'other';
      case 'ro': case 'mo':
        // romanian
        var rem100 = c % 100;
        if (c == 1) return 'one';
        else if ((c == 0 || (rem100 >= 1 && rem100 <= 19))) return 'few';
        else return 'other';
      case 'sl':
        // slovenian
        var rem100 = c % 100;
        if (rem100 == 1) return 'one';
        else if (rem100 == 2) return 'two';
        else if (rem100 >= 3 && rem100 <= 4) return 'few';
        else return 'other';
      case 'ar':
        // arabic
        var rem100 = c % 100;
        if (c == 0) return 'zero';
        else if (c == 1) return 'one';
        else if (c == 2) return 'two';
        else if (rem100 >= 3 && rem100 <= 10) return 'few';
        else if (rem100 >= 11 && rem100 <= 99) return 'many';
        else return 'other';
      case 'mk':
        // macedonian
        return (c % 10 == 1 && c != 11) ? 'one' : 'other';
      case 'cy': case 'br':
        // welsh
        if (c == 0) return 'zero';
        else if (c == 1) return 'one';
        else if (c == 2) return 'two';
        else if (c == 3) return 'few';
        else if (c == 6) return 'many';
        else return 'other';
      case 'mt':
        // maltese
        var rem100 = c % 100;
        if (c == 1) return 'one';
        else if (c == 0 || (rem100 >= 2 && rem100 <= 10)) return 'few';
        else if (rem100 >= 11 && rem100 <= 19) return 'many';
        else return 'other';
      case 'ga': case 'se': case 'sma': case 'smi': case 'smj': case 'smn':
      case 'sms':
        // two
        if (c == 1) return 'one';
        else if (c == 2) return 'two';
        else return 'other';
      case 'ak': case 'am': case 'bh': case 'fil': case 'tl': case 'guw':
      case 'hi': case 'pl': case 'mg': case 'nso': case 'ti': case 'wa':
        // zero
        return (c == 0 || c == 1) ? 'one' : 'other';
      case 'az': case 'bm': case 'fa': case 'ig': case 'hu': case 'ja':
      case 'kde': case 'kea': case 'ko': case 'my': case 'ses': case 'sg':
      case 'to': case 'tr': case 'vi': case 'wo': case 'yo': case 'zh':
      case 'bo': case 'dz': case 'id': case 'jv': case 'ka': case 'km':
      case 'kn': case 'ms': case 'th': case 'tp': case 'io': case 'ia':
        // none
        return 'other';
      default:
        return (c == 1) ? 'one' : 'other';
    }
  };

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
    var pluralKey = key + ':' + quantity(count);
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
