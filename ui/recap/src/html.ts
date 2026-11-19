import { numberFormat } from 'lib/i18n';

export const fmt = (n: number): string => numberFormat(Math.round(n));

export const esc = (s: string | number): string =>
  String(s).replace(
    /[&<>"']/g,
    c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!,
  );

// trusted html fragment, to interpolate markup into translated strings
export interface Raw {
  html: string;
}
export const raw = (html: string): Raw => ({ html });

// renders the output of `i18n.x.asArray(raw(...))`: text is escaped, raw fragments are not
export const trHtml = (parts: (string | Raw)[]): string =>
  parts.map(p => (typeof p === 'string' ? esc(p) : p.html)).join('');

// the words around the number of a plural, e.g. "games" from "%s games", so the number can be shown on its own
export const pluralLabel = (plural: I18nPlural, n: number): string => {
  const mark = raw('');
  return plural
    .asArray(n, mark)
    .filter(p => p !== mark)
    .join('')
    .trim();
};

// text revealed word by word
export const words = (text: string, delay = 0, step = 0.06): string =>
  text
    .split(' ')
    .map((w, i) => `<span class="word" style="--d:${(delay + i * step).toFixed(2)}s">${esc(w)}</span>`)
    .join(' ');

export const countSpan = (n: number, delay = 0, tag = 'strong', cls = 'num'): Raw =>
  raw(`<${tag} class="${cls}" data-count="${n}" data-delay="${delay}">0</${tag}>`);

export const easeOutExpo = (t: number): number => (t >= 1 ? 1 : 1 - Math.pow(2, -10 * t));
export const easeInOut = (t: number): number => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);
export const rand = (a: number, b: number): number => a + Math.random() * (b - a);
export const gauss = (): number => (Math.random() + Math.random() + Math.random() + Math.random() - 2) / 2;
export const pick = <A>(arr: ArrayLike<A>): A => arr[Math.floor(Math.random() * arr.length)];

export const pieceSrc = (color: 'w' | 'b', role: string): string =>
  site.asset.url(`piece/cburnett/${color}${role}.svg`);

export const reduceMotion = (): boolean => matchMedia('(prefers-reduced-motion: reduce)').matches;

export const userLabel = (u: LightUser): string =>
  `${u.title ? `<span class="utitle">${esc(u.title)}</span> ` : ''}${esc(u.name)}`;

const fallbackFlairs = new Map<string, Flair>();
export const flairOf = (u: LightUser): Flair => {
  if (u.flair) return u.flair;
  if (!fallbackFlairs.has(u.id))
    fallbackFlairs.set(
      u.id,
      pick<Flair>(['activity.lichess-horsey', 'activity.lichess-hogger', 'activity.lichess-horsey-yin-yang']),
    );
  return fallbackFlairs.get(u.id)!;
};
