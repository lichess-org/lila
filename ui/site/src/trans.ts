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
      for (let i = 0; i < args.length; i++) {
        const placeholder = segments.indexOf('%' + (i + 1) + '$s');
        if (placeholder !== -1) segments[placeholder] = args[i];
      }
  }
  return segments;
}

export const trans = (i18n: I18nDict) => {
  const trans: Trans = (key: I18nKey, ...args: Array<string | number>) => {
    const str = i18n[key];
    return str ? format(str, args) : key;
  };

  // see optimisations in project/MessageCompiler.scala
  const resolvePlural = (key: I18nKey, count: number) =>
    i18n[`${key}:${site.quantity(count)}`] || i18n[`${key}:other`] || i18n[key] || i18n[`${key}:one`];

  trans.pluralSame = (key: I18nKey, count: number, ...args: Array<string | number>) =>
    trans.plural(key, count, count, ...args);

  trans.plural = function (key: I18nKey, count: number, ...args: Array<string | number>) {
    const str = resolvePlural(key, count);
    return str ? format(str, args) : key;
  };
  // optimisation for translations without arguments
  trans.noarg = (key: I18nKey) => i18n[key] || key;
  trans.vdom = <T>(key: I18nKey, ...args: T[]) => {
    const str = i18n[key];
    return str ? list(str, args) : [key];
  };
  trans.vdomPlural = <T>(key: I18nKey, count: number, ...args: T[]) => {
    const str = resolvePlural(key, count);
    return str ? list(str, args) : [key];
  };
  return trans;
};

export const siteTrans = trans(window.site?.siteI18n || {});
