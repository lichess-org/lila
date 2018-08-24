function format(str: string, args: Array<string | number>): string {
  for (let i = 1; str.indexOf('$s') > -1 && i < 4 && i - 1 < args.length; i++) {
    str = str.replace('%' + i + '$s', args[i - 1].toString());
  }
  for (let i = 0; i < args.length; i++) {
    str = str.replace('%s', args[i].toString());
  }
  return str;
}

function list<T>(str: string, args: T[]): Array<string | T> {
  const segments: Array<string | T> = str.split(/(%(?:\d\$)?s)/g);
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
}

export default function trans(i18n: { [key: string]: string | undefined }): Trans {
  const trans: any = function(key: string, ...args: Array<string | number>): string {
    const str = i18n[key];
    return str ? format(str, args) : key;
  };
  trans.plural = function(key: string, count: number, ..._args: Array<string | number>) {
    const pluralKey = key + ':' + window.lichess.quantity(count);
    const str = i18n[pluralKey] || i18n[key];
    return str ? format(str, Array.prototype.slice.call(arguments, 1)) : key;
  };
  trans.noarg = function(key: string) {
    // optimisation for translations without arguments
    return i18n[key] || key;
  };
  trans.vdom = function<T>(key: string, ...args: T[]): Array<string | T> {
    const str = i18n[key];
    return str ? list(str, args) : [key];
  };
  trans.vdomPlural = function<T>(key: string, count: number, ...args: T[]): Array<string | T> {
    const pluralKey = key + ':' + window.lichess.quantity(count);
    const str = i18n[pluralKey] || i18n[key];
    return str ? list(str, args) : [key];
  };
  return trans;
}
