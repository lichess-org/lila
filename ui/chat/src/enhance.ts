export default function(text: string, parseMoves: boolean): string {
  const escaped = window.lichess.escapeHtml(text);
  const linked = autoLink(escaped);
  const plied = parseMoves && linked === escaped ? addPlies(linked) : linked;
  return plied;
}

const linkPattern = /(^|[\s\n]|<[A-Za-z]*\/?>)((?:(?:https?):\/\/|lichess\.org\/)[\-A-Z0-9+\u0026\u2019@#\/%?=()~_|!:,.;]*[\-A-Z0-9+\u0026@#\/%=~()_|])/gi;

function linkReplace(match: string, before: string, url: string) {
  if (url.indexOf('@') !== -1 || url.indexOf('&quot;') !== -1) return match;
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
  return html.replace(linkPattern, linkReplace).replace(userPattern, userLinkReplace);
}

const movePattern = /\b(\d+)\s?(\.+)\s?(?:[o0-]+|[NBRQK]*[a-h]?[1-8]?x?@?[a-h][0-9]=?[NBRQK]?)\+?\#?[!\?=]*/gi;
function moveReplacer(match: string, turn: number, dots: string) {
  if (turn < 1 || turn > 200) return match;
  const ply = turn * 2 - (dots.length > 1 ? 0 : 1);
  return '<a class="jump" data-ply="' + ply + '">' + match + '</a>';
}

function addPlies(html: string) {
  return html.replace(movePattern, moveReplacer);
}
