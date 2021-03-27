const trans = (i18n: I18nDict) => {
  const format = (str: string, args: string[]) => {
    if (args.length && str.includes('$s')) for (var i = 1; i < 4; i++) str = str.replace('%' + i + '$s', args[i - 1]);
    args.forEach(function (arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
  const list = (str: string, args: string[]) => {
    const segments = str.split(/(%(?:\d\$)?s)/g);
    for (let i = 1; i <= args.length; i++) {
      const pos = segments.indexOf('%' + i + '$s');
      if (pos !== -1) segments[pos] = args[i - 1];
    }
    for (let i = 0; i < args.length; i++) {
      const pos = segments.indexOf('%s');
      if (pos === -1) break;
      segments[pos] = args[i];
    }
    return segments;
  };

  const trans: Trans = function (key: string) {
    const str = i18n[key];
    return str ? format(str, Array.prototype.slice.call(arguments, 1)) : key;
  };
  trans.plural = function (key: string, count: number) {
    const pluralKey = `${key}:${lichess.quantity(count)}`;
    const str = i18n[pluralKey] || i18n[key];
    return str ? format(str, Array.prototype.slice.call(arguments, 1)) : key;
  };
  // optimisation for translations without arguments
  trans.noarg = (key: string) => i18n[key] || key;
  trans.vdom = function (key: string) {
    const str = i18n[key];
    return str ? list(str, Array.prototype.slice.call(arguments, 1)) : [key];
  };
  trans.vdomPlural = function (key: string, count: number) {
    const pluralKey = `${key}:${lichess.quantity(count)}`;
    const str = i18n[pluralKey] || i18n[key];
    return str ? list(str, Array.prototype.slice.call(arguments, 2)) : [key];
  };
  return trans;
};

export default trans;
