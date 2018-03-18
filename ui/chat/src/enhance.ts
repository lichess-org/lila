import { addPlies } from 'common';

export default function(text: string, parseMoves: boolean): string {
  const escaped = window.lichess.escapeHtml(text);
  const linked = autoLink(escaped);
  const plied = parseMoves && linked === escaped ? addPlies(linked) : linked;
  return plied;
}

const linkPattern = /(^|[\s\n]|<[A-Za-z]*\/?>)((?:(?:https?):\/\/|lichess\.org\/)[\-A-Z0-9+\u0026\u2019@#\/%?=()~_|!:,.;]*[\-A-Z0-9+\u0026@#\/%=~()_|])/gi;

function linkReplace(match: string, before: string, url: string) {
  if (url.indexOf('&quot;') !== -1) return match;
  const fullUrl = url.indexOf('http') === 0 ? url : 'https://' + url;
  const minUrl = url.replace(/^(?:https:\/\/)?(.+)$/, '$1');
  return before + '<a target="_blank" rel="nofollow" href="' + fullUrl + '">' + minUrl + '</a>';
}

const userPattern = /(^|[^\w@#/])@([\w-]{2,})/g

function userLinkReplace(orig: string, prefix: String, user: string) {
  if (user.length > 20) return orig;
  return prefix + '<a href="/@/' + user + '">@' + user + "</a>";
}

function autoLink(html: string) {
  return html.replace(userPattern, userLinkReplace).replace(linkPattern, linkReplace);
}
