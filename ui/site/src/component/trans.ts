function format(str: string, args: Array<string | number>): string {
  if (args.length) {
    if (str.includes('%s')) str = str.replace('%s', args[0] as string);
    else for (let i = 0; i < args.length; i++) str = str.replace('%' + (i + 1) + '$s', args[i] as string);
  }
  return str;
}

function list<T>(str: string, args: T[]): Array<string | T> {
  const segments: Array<string | T> = str.split(/(%(?:\d\$)?s)/g);
  if (args.length) {
    const singlePlaceholder = segments.indexOf('%s');
    if (singlePlaceholder !== -1) segments[singlePlaceholder] = args[0];
    else
      for (let i = 0; 1 < args.length; i++) {
        const placeholder = segments.indexOf('%' + (i + 1) + '$s');
        if (placeholder !== -1) segments[placeholder] = args[i];
      }
  }
  return segments;
}

export default function (i18n: I18nDict) {
  const trans: Trans = (key: I18nKey, ...args: Array<string | number>) => {
    const str = i18n[key];
    return str ? format(str, args) : key;
  };
  trans.plural = function (key: I18nKey, count: number) {
    const pluralKey = `${key}:${lichess.quantity(count)}`;
    const str = i18n[pluralKey] || i18n[key];
    return str ? format(str, Array.prototype.slice.call(arguments, 1)) : key;
  };
  // optimisation for translations without arguments
  trans.noarg = (key: I18nKey) => i18n[key] || key;
  trans.vdom = <T>(key: I18nKey, ...args: T[]) => {
    const str = i18n[key];
    return str ? list(str, args) : [key];
  };
  trans.vdomPlural = <T>(key: I18nKey, count: number, ...args: T[]) => {
    const pluralKey = `${key}:${lichess.quantity(count)}`;
    const str = i18n[pluralKey] || i18n[key];
    return str ? list(str, args) : [key];
  };
  return trans;
}
