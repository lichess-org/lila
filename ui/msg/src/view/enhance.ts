export function enhance(text: string): string {
  const escaped = window.lichess.escapeHtml(text);
  return nl2br(autoLink(escaped));
}

export function isMoreThanText(str: string) {
  return moreThanTextPattern.test(str) || possibleLinkPattern.test(str);
}

const moreThanTextPattern = /[&<>"@\n]/;
const possibleLinkPattern = /\.\w/;
const linkPattern = /\b(https?:\/\/|lichess\.org\/)[-–—\w+&'@#\/%?=()~|!:,.;]+[\w+&@#\/%=~|]/gi;

function linkReplace(url: string, scheme: string) {
  if (url.includes('&quot;')) return url;
  const fullUrl = scheme === 'lichess.org/' ? 'https://' + url : url;
  const minUrl = url.replace(/^https:\/\//, '');
  return '<a target="_blank" rel="nofollow" href="' + fullUrl + '">' + minUrl + '</a>';
}

const userPattern = /(^|[^\w@#/])(@|(?:https:\/\/)?lichess\.org\/@\/)([\w-]{2,})/g;

function userLinkReplace(_orig: string, prefix: String, _scheme: String, user: string) {
  return prefix + '<a href="/@/' + user + '">@' + user + "</a>";
}

function autoLink(html: string) {
  return html.replace(userPattern, userLinkReplace).replace(linkPattern, linkReplace);
}

function nl2br(html: string) {
  return html.replace(/\n/g, '<br>');
}
