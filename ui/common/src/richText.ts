// Rich Text helper functions
// Refactored for https://github.com/lichess-org/lila/issues/7342 request
import { VNode, Hooks } from 'snabbdom';
import { escapeHtml } from './common';

// from https://github.com/bryanwoods/autolink-js/blob/master/autolink.js
export const linkRegex =
  /(^|[\s\n]|<[A-Za-z]*\/?>)((?:(?:https?|ftp):\/\/|lichess\.org)[\-A-Z0-9+\u0026\u2019@#\/%?=()~_|!:,.;]*[\-A-Z0-9+\u0026@#\/%=~()_|])/gi;
export const newLineRegex = /\n/g;
export const userPattern = /(^|[^\w@#/])@([a-z0-9_-]{2,30})/gi;

// looks like it has a @mention or #gameid or a url.tld
export const isMoreThanText = (str: string) => /(\n|(@|#|\.)\w{2,}|(board|game) \d)/i.test(str);

export function toLink(url: string): string {
  if (!url.match(/^[A-Za-z]+:\/\//)) url = 'https://' + url;
  return `<a target="_blank" rel="nofollow noopener noreferrer" href="${url}">${url.replace(
    /https?:\/\//,
    '',
  )}</a>`;
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

export function linkReplace(href: string, body?: string, cls?: string) {
  if (href.includes('&quot;')) return href;
  return `<a target="_blank" rel="noopener nofollow noreferrer" href="${
    href.startsWith('/') || href.includes('://') ? href : '//' + href
  }"${cls ? ` class="${cls}"` : ''}>${body ? body : href}</a>`;
}

export const userLinkReplace = (_: string, prefix: string, user: string) =>
  prefix + linkReplace('/@/' + user, '@' + user);

export const expandMentions = (html: string) => html.replace(userPattern, userLinkReplace);

export function enrichText(text: string, allowNewlines = true): string {
  let html = autolink(escapeHtml(text), toLink);
  if (allowNewlines) html = html.replace(newLineRegex, '<br>');
  return html;
}

export function richHTML(text: string, newLines = true): Hooks {
  return innerHTML(text, t => enrichText(t, newLines));
}

const linkPattern = /\b\b(?:https?:\/\/)?(lichess\.org\/[-–—\w+&'@#\/%?=()~|!:,.;]+[\w+&@#\/%=~|])/gi;
const pawnDropPattern = /^[a-h][2-7]$/;
export const movePattern =
  /\b(\d+)\s*(\.+)\s*(?:[o0-]+[o0]|[NBRQKP\u2654\u2655\u2656\u2657\u2658\u2659]?[a-h]?[1-8]?[x@]?[a-h][1-8](?:=[NBRQK\u2654\u2655\u2656\u2657\u2658\u2659])?)\+?#?[!\?=]{0,5}/gi;
const boardPattern = /\b(?:board|game)\s(\d{1,2})/gi;

function moveReplacer(match: string, turn: number, dots: string) {
  if (turn < 1 || turn > 200) return match;
  const ply = turn * 2 - (dots.length > 1 ? 0 : 1);
  return '<a class="jump" data-ply="' + ply + '">' + match + '</a>';
}
function boardReplacer(match: string, board: number) {
  if (board < 1 || board > 64) return match;
  return '<a data-board="' + board + '">' + match + '</a>';
}

const addPlies = (html: string) => html.replace(movePattern, moveReplacer);
const addBoards = (html: string) => html.replace(boardPattern, boardReplacer);

const userLinkReplacePawn = (orig: string, prefix: string, user: string) =>
  user.match(pawnDropPattern) ? orig : userLinkReplace(orig, prefix, user);

export interface EnhanceOpts {
  plies: boolean;
  boards?: boolean;
}

export function enhance(text: string, opts?: EnhanceOpts): string {
  const escaped = escapeHtml(text);
  const linked = escaped.replace(userPattern, userLinkReplacePawn).replace(linkPattern, linkReplace);
  const plied = opts?.plies && linked === escaped ? addPlies(linked) : linked;
  const boarded = opts?.boards && linked === escaped ? addBoards(plied) : linked;
  return boarded;
}
