export function enhance(text: string, parseMoves: boolean): string {
  const escaped = window.lishogi.escapeHtml(text);
  const linked = autoLink(escaped);
  const plied = parseMoves && linked === escaped ? addPlies(linked) : linked;
  return plied;
}

const moreThanTextPattern = /[&<>"@]/;
const possibleLinkPattern = /\.\w/;
export const possibleTitlePattern = /^\[.*?\]/;

export function isMoreThanText(str: string, system: boolean) {
  return moreThanTextPattern.test(str) || possibleLinkPattern.test(str) || (system && possibleTitlePattern.test(str));
}

const linkPattern = /\b(https?:\/\/|lishogi\.org\/)[-–—\w+&'@#\/%?=()~|!:,.;]+[\w+&@#\/%=~|]/gi;

function linkReplace(url: string, scheme: string) {
  if (url.includes('&quot;')) return url;
  const fullUrl = scheme === 'lishogi.org/' ? 'https://' + url : url;
  const minUrl = url.replace(/^https:\/\//, '');
  return '<a target="_blank" rel="nofollow noopener noreferrer" href="' + fullUrl + '">' + minUrl + '</a>';
}

const userPattern = /(^|[^\w@#/])@([\w-]{2,})/g;

function userLinkReplace(orig: string, prefix: String, user: string) {
  if (user.length > 20) return orig;
  return prefix + '<a href="/@/' + user + '">@' + user + '</a>';
}

function autoLink(html: string) {
  return html.replace(userPattern, userLinkReplace).replace(linkPattern, linkReplace);
}

const movePattern = /\b(\d+)\s*(\.)\s*(([1-9][a-i])[1-9][a-i](\+|=)?)[!\?]{0,5}/gi;
function moveReplacer(match: string, turn: number) {
  if (turn < 1 || turn > 200) return match;
  const ply = turn - 1;
  return '<a class="jump" data-ply="' + ply + '">' + match + '</a>';
}

function addPlies(html: string) {
  return html.replace(movePattern, moveReplacer);
}
