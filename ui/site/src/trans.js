lichess.trans = function(i18n) {
  var lang = document.documentElement.lang;

  var p = {};
  p.fr = p.ff = p.kab = function(c) {
    // french
    return (c < 2) ? 'one' : 'other';
  };
  p.cs = p.sk = function(c) {
    // czech
    if (c == 1) return 'one';
    else if (c >= 2 && c <= 4) return 'few';
    else return 'other';
  };
  p.hr = p.ru = p.sr = p.uk = p.be = p.bs = p.sh = function(c) {
    // balkan
    var rem100 = c % 100;
    var rem10 = c % 10;
    if (rem10 == 1 && rem100 != 11) return 'one';
    else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) return 'few';
    else if (rem10 == 0 || (rem10 >= 5 && rem10 <= 9) || (rem100 >= 11 && rem100 <= 14)) return 'many';
    else return 'other';
  };
  p.lv = function(c) {
    // latvian
    if (c == 0) return 'zero';
    else if (c % 10 == 1 && c % 100 != 11) return 'one';
    else return 'other';
  };
  p.lt = function(c) {
    // lithuanian
    var rem100 = c % 100;
    var rem10 = c % 10;
    if (rem10 == 1 && !(rem100 >= 11 && rem100 <= 19)) return 'one';
    else if (rem10 >= 2 && rem10 <= 9 && !(rem100 >= 11 && rem100 <= 19)) return 'few';
    else return 'other';
  };
  p.pl = function(c) {
    // polish
    var rem100 = c % 100;
    var rem10 = c % 10;
    if (c == 1) return 'one';
    else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) return 'few';
    else return 'other';
  };
  p.ro = p.mo = function(c) {
    // romanian
    var rem100 = c % 100;
    if (c == 1) return 'one';
    else if ((c == 0 || (rem100 >= 1 && rem100 <= 19))) return 'few';
    else return 'other';
  };
  p.sl = function(c) {
    // slovenian
    var rem100 = c % 100;
    if (rem100 == 1) return 'one';
    else if (rem100 == 2) return 'two';
    else if (rem100 >= 3 && rem100 <= 4) return 'few';
    else return 'other';
  };
  p.ar = function(c) {
    // arabic
    var rem100 = c % 100;
    if (c == 0) return 'zero';
    else if (c == 1) return 'one';
    else if (c == 2) return 'two';
    else if (rem100 >= 3 && rem100 <= 10) return 'few';
    else if (rem100 >= 11 && rem100 <= 99) return 'many';
    else return 'other';
  };
  p.mk = function(c) {
    // macedonian
    return (c % 10 == 1 && c != 11) ? 'one' : 'other';
  };
  p.cy = p.br = function(c) {
    // welsh
    if (c == 0) return 'zero';
    else if (c == 1) return 'one';
    else if (c == 2) return 'two';
    else if (c == 3) return 'few';
    else if (c == 6) return 'many';
    else return 'other';
  };
  p.mt = function(c) {
    // maltese
    var rem100 = c % 100;
    if (c == 1) return 'one';
    else if (c == 0 || (rem100 >= 2 && rem100 <= 10)) return 'few';
    else if (rem100 >= 11 && rem100 <= 19) return 'many';
    else return 'other';
  };
  p.ga = p.se = p.sma = p.smi = p.smj = p.smn = p.sms = function(c) {
    // two
    if (c == 1) return 'one';
    else if (c == 2) return 'two';
    else return 'other';
  };
  p.ak = p.am = p.bh = p.fil = p.tl = p.guw = p.hi = p.pl = p.mg = p.nso = p.ti = p.wa = function(c) {
    // zero
    return (c == 0 || c == 1) ? 'one' : 'other';
  };
  p.az = p.bm = p.fa = p.ig = p.hu = p.ja = p.kde = p.kea = p.ko = p.my = p.ses = p.sg = p.to = p.tr = p.vi = p.wo = p.yo = p.zh = p.bo = p.dz = p.id = p.jv = p.ka = p.km = p.kn = p.ms = p.th = p.tp = p.io = p.ia = function(c) {
    // none
    return 'other';
  };

  var plural = p[lang] || function(c) {
    // default
    return (c == 1) ? 'one' : 'other';
  };

  var format = function(str) {
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

  var trans = function(key) {
    return i18n[key] ? i18n[key] : key;
  };
  trans.plural = function(key, quantity) {
    var pluralKey = key + ':' + plural(quantity);
    var str = i18n[pluralKey] || i18n[key + ':other'] || i18n[key];
    return str ? format(str, quantity) : key;
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
