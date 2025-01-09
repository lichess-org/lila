// Taken from https://github.com/lichess-org/lila/blob/9c8c964037dac0e71728af3a66ef515fac6e8997/ui/common/src/richText.ts
import type { VNode, Hooks } from 'snabbdom';
import { escapeHtml } from './string';

// from https://github.com/bryanwoods/autolink-js/blob/master/autolink.js
export const linkRegex: RegExp =
  /(^|[\s\n]|<[A-Za-z]*\/?>)((?:(?:https?|ftp):\/\/|lishogi\.org)[\-A-Z0-9+\u0026\u2019@#\/%?=()~_|!:,.;]*[\-A-Z0-9+\u0026@#\/%=~()_|])/gi;
export const newLineRegex: RegExp = /\n/g;
export const userPattern: RegExp = /(^|[^\w@#/])@([a-z0-9_-]{2,30})/gi;

// looks like it has a @mention or #gameid or a url.tld
export const isMoreThanText = (str: string): boolean => /(\n|(@|#|\.)\w{2,})|(:\d+\/)/.test(str);

const linkHtml = (href: string, content: string): string =>
  `<a target="_blank" rel="nofollow noopener noreferrer" href="${href}">${content}</a>`;

export function toLink(url: string): string {
  if (!url.match(/^[A-Za-z]+:\/\//)) url = 'https://' + url;
  return linkHtml(url, url.replace(/https?:\/\//, ''));
}

export const autolink = (str: string, callback: (str: string) => string): string =>
  str.replace(linkRegex, (_, space, url) => space + callback(url));

export const innerHTML = <A>(a: A, toHtml: (a: A) => string): Hooks => ({
  insert(vnode: VNode) {
    (vnode.elm as HTMLElement).innerHTML = toHtml(a);
    vnode.data!.cachedA = a;
  },
  postpatch(old: VNode, vnode: VNode) {
    if (old.data!.cachedA !== a) {
      (vnode.elm as HTMLElement).innerHTML = toHtml(a);
    }
    vnode.data!.cachedA = a;
  },
});

export function linkReplace(href: string, body?: string): string {
  if (href.includes('&quot;')) return href;
  return linkHtml(
    href.startsWith('/') || href.includes('://') ? href : '//' + href,
    body ? body : href,
  );
}

export const userLinkReplace = (_: string, prefix: string, user: string): string =>
  prefix + linkReplace('/@/' + user, '@' + user);

export const expandMentions = (html: string): string => html.replace(userPattern, userLinkReplace);

export function enrichText(text: string, allowNewlines = true): string {
  let html = autolink(escapeHtml(text), toLink);
  if (allowNewlines) html = html.replace(newLineRegex, '<br>');
  return html;
}

export function richHTML(text: string, newLines = true): Hooks {
  return innerHTML(text, t => enrichText(t, newLines));
}

const linkPattern =
  /\b\b(?:https?:\/\/)?(lishogi\.org\/[-–—\w+&'@#\/%?=()~|!:,.;]+[\w+&@#\/%=~|])/gi;
const movePattern = /\b(\d+)\s*(\.)\s*(([1-9][a-i])[1-9][a-i](\+|=)?)[!\?]{0,5}/gi;
function moveReplacer(match: string, turn: number) {
  if (turn < 1 || turn > 200) return match;
  const ply = turn - 1;
  return '<a class="jump" data-ply="' + ply + '">' + match + '</a>';
}

const addPlies = (html: string) => html.replace(movePattern, moveReplacer);

export function enhance(text: string, parseMoves: boolean): string {
  const escaped = escapeHtml(text);
  const linked = escaped.replace(userPattern, userLinkReplace).replace(linkPattern, linkReplace);
  const plied = parseMoves && linked === escaped ? addPlies(linked) : linked;
  return plied;
}
