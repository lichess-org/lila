export function enhance(text: string, parseMoves: boolean): string {
  const escaped = window.lidraughts.escapeHtml(text);
  const linked = autoLink(escaped);
  const plied = parseMoves && linked === escaped ? addPlies(linked) : linked;
  return plied;
}

const moreThanTextPattern = /[&<>"@]/;
const possibleLinkPattern = /\.\w/;

export function isMoreThanText(str: string) {
  return moreThanTextPattern.test(str) || possibleLinkPattern.test(str);
}

const linkPattern = /\b(https?:\/\/|lidraughts\.org\/)[-–—\w+&'@#\/%?=()~|!:,.;]+[\w+&@#\/%=~|]/gi;

function linkReplace(url: string, scheme: string) {
  if (url.includes('&quot;')) return url;
  const fullUrl = scheme === 'lidraughts.org/' ? 'https://' + url : url;
  const minUrl = url.replace(/^https:\/\//, '');
  return '<a target="_blank" rel="nofollow noopener noreferrer" href="' + fullUrl + '">' + minUrl + '</a>';
}

const userPattern = /(^|[^\w@#/])(@|(?:https:\/\/)?lidraughts\.org\/@\/)([\w-]{2,})/g;
const pawnDropPattern = /^[a-h][2-7]$/;

function userLinkReplace(orig: string, prefix: String, scheme: String, user: string) {
  if (user.length > 20 || (scheme === '@' && user.match(pawnDropPattern))) return orig;
  return prefix + '<a href="/@/' + user + '">@' + user + "</a>";
}

function autoLink(html: string) {
  return html.replace(userPattern, userLinkReplace).replace(linkPattern, linkReplace);
}

const movePattern = /\b(\d+)\s*(\.+)\s*(?:\d{1,2}[x-]{1}\d{1,2}|\d{4})[!\?=]{0,5}/gi;
function moveReplacer(match: string, turn: number, dots: string) {
  if (turn < 1 || turn > 200) return match;
  const ply = turn * 2 - (dots.length > 1 ? 0 : 1);
  return '<a class="jump" data-ply="' + ply + '">' + match + '</a>';
}

function addPlies(html: string) {
  return html.replace(movePattern, moveReplacer);
}