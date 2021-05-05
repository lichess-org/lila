// Rich Text helper functions
// Refactored for https://github.com/ornicar/lila/issues/7342 request
import { VNode, Hooks } from 'snabbdom';

// from https://github.com/bryanwoods/autolink-js/blob/master/autolink.js
export const linkRegex = /(^|[\s\n]|<[A-Za-z]*\/?>)((?:(?:https?|ftp):\/\/|lichess\.org)[\-A-Z0-9+\u0026\u2019@#\/%?=()~_|!:,.;]*[\-A-Z0-9+\u0026@#\/%=~()_|])/gi;
export const newLineRegex = /\n/g;
export const userPattern = /(^|[^\w@#/])@([a-z0-9][a-z0-9_-]{0,28}[a-z0-9])/gi;

// looks like it has a @mention or #gameid or a url.tld
export function isMoreThanText(str: string) {
  return /(\n|(@|#|\.)\w{2,})/.test(str);
}

export function toLink(url: string): string {
  return `<a target="_blank" rel="nofollow noopener noreferrer" href="${url}">${url.replace(/https?:\/\//, '')}</a>`;
}

export function autolink(str: string, callback: (str: string) => string): string {
  return str.replace(linkRegex, (_, space, url) => space + callback(url));
}

export function innerHTML<A>(a: A, toHtml: (a: A) => string): Hooks {
  return {
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
  };
}

export function linkReplace(href: string, body?: string, cls?: string) {
  if (href.includes('&quot;')) return href;
  return `<a target="_blank" rel="noopener nofollow noreferrer" href="${
    href.startsWith('/') || href.includes('://') ? href : '//' + href
  }"${cls ? ` class="${cls}"` : ''}>${body ? body : href}</a>`;
}

export function userLinkReplace(_: string, prefix: string, user: string) {
  return prefix + linkReplace('/@/' + user, '@' + user);
}

export function enrichText(text: string, allowNewlines = true): string {
  let html = autolink(lichess.escapeHtml(text), toLink);
  if (allowNewlines) html = html.replace(newLineRegex, '<br>');
  return html;
}

export function richHTML(text: string, newLines = true): Hooks {
  return innerHTML(text, t => enrichText(t, newLines));
}

const linkPattern = /\b\b(?:https?:\/\/)?(lichess\.org\/[-–—\w+&'@#\/%?=()~|!:,.;]+[\w+&@#\/%=~|])/gi;
const pawnDropPattern = /^[a-h][2-7]$/;
const movePattern = /\b(\d+)\s*(\.+)\s*(?:[o0-]+[o0]|[NBRQKP\u2654\u2655\u2656\u2657\u2658\u2659]?[a-h]?[1-8]?[x@]?[a-z][1-8](?:=[NBRQK\u2654\u2655\u2656\u2657\u2658\u2659])?)\+?#?[!\?=]{0,5}/gi;

function moveReplacer(match: string, turn: number, dots: string) {
  if (turn < 1 || turn > 200) return match;
  const ply = turn * 2 - (dots.length > 1 ? 0 : 1);
  return '<a class="jump" data-ply="' + ply + '">' + match + '</a>';
}

function addPlies(html: string) {
  return html.replace(movePattern, moveReplacer);
}

function userLinkReplacePawn(orig: string, prefix: string, user: string) {
  if (user.match(pawnDropPattern)) return orig;
  return userLinkReplace(orig, prefix, user);
}

export function enhance(text: string, parseMoves: boolean): string {
  const escaped = lichess.escapeHtml(text);
  const linked = escaped.replace(userPattern, userLinkReplacePawn).replace(linkPattern, linkReplace);
  const plied = parseMoves && linked === escaped ? addPlies(linked) : linked;
  return plied;
}

function toYouTubeEmbedUrl(url: string) {
  if (!url) return;
  const m = url.match(
    /(?:https?:\/\/)?(?:www\.)?(?:youtube\.com|youtu\.be)\/(?:watch)?(?:\?v=)?([^"&?\/ ]{11})(?:\?|&|)(\S*)/i
  );
  if (!m) return;
  let start = 0;
  m[2].split('&').forEach(function (p) {
    const s = p.split('=');
    if (s[0] === 't' || s[0] === 'start') {
      if (s[1].match(/^\d+$/)) start = parseInt(s[1]);
      else {
        const n = s[1].match(/(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?/)!;
        start = (parseInt(n[1]) || 0) * 3600 + (parseInt(n[2]) || 0) * 60 + (parseInt(n[3]) || 0);
      }
    }
  });
  const params = 'modestbranding=1&rel=0&controls=2&iv_load_policy=3' + (start ? '&start=' + start : '');
  return 'https://www.youtube.com/embed/' + m[1] + '?' + params;
}

export function toYouTubeEmbed(url: string): string | undefined {
  const embedUrl = toYouTubeEmbedUrl(url);
  if (embedUrl)
    return `<div class="embed"><iframe width="100%" src="${embedUrl}" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe></div>`;
  return undefined;
}

function toTwitchEmbedUrl(url: string) {
  if (!url) return;
  const m = url.match(/(?:https?:\/\/)?(?:www\.)?(?:twitch.tv)\/([^"&?/ ]+)/i);
  if (m) return `https://player.twitch.tv/?channel=${m[1]}&parent=${location.hostname}&autoplay=false`;
  return undefined;
}

export function toTwitchEmbed(url: string): string | undefined {
  const embedUrl = toTwitchEmbedUrl(url);
  if (embedUrl)
    return `<div class="embed"><iframe width="100%" src="${embedUrl}" frameborder=0 allowfullscreen></iframe></div>`;
  return undefined;
}
