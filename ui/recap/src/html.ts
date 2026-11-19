import { numberFormat } from 'lib/i18n';

export const fmt = (n: number): string => numberFormat(Math.round(n));

// for text that changes every frame: editing the text node, rather than replacing it,
// spares the browser from restyling the page around it
export const setText = (el: Element, text: string): void => {
  const node = el.firstChild;
  if (node instanceof Text && !node.nextSibling) {
    if (node.data !== text) node.data = text;
  } else el.textContent = text;
};

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

const roleNames: Record<string, string> = {
  K: 'king',
  Q: 'queen',
  R: 'rook',
  B: 'bishop',
  N: 'knight',
  P: 'pawn',
};

// the viewer's piece set, from the ---white-king etc. variables the page sets
const pieceSrcs = new Map<string, string>();
export const pieceSrc = (color: 'w' | 'b', role: string): string => {
  const key = color + role;
  let src = pieceSrcs.get(key);
  if (!src) {
    const css = getComputedStyle(document.body).getPropertyValue(
      `---${color === 'w' ? 'white' : 'black'}-${roleNames[role]}`,
    );
    src = css.match(/url\(["']?(.+?)["']?\)/)?.[1] ?? site.asset.url(`piece/cburnett/${key}.svg`);
    pieceSrcs.set(key, src);
  }
  return src;
};

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
