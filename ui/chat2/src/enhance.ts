var linkPattern = /(^|[\s\n]|<[A-Za-z]*\/?>)((?:(?:https?):\/\/|lichess\.org\/)[\-A-Z0-9+\u0026\u2019@#\/%?=()~_|!:,.;]*[\-A-Z0-9+\u0026@#\/%=~()_|])/gi;

function linkReplace(match: string, before: string, url: string) {
  var fullUrl = url.indexOf('http') === 0 ? url : 'https://' + url;
  var minUrl = url.replace(/^(?:https:\/\/)?(.+)$/, '$1');
  return before + '<a target="_blank" rel="nofollow" href="' + fullUrl + '">' + minUrl + '</a>';
}

function autoLink(html: string) {
  return html.replace(linkPattern, linkReplace);
}

var delocalizePattern = /(^|[\s\n]|<[A-Za-z]*\/?>)\w{2}\.lichess\.org/gi;

function delocalize(html: string) {
  return html.replace(delocalizePattern, '$1lichess.org');
}

function escapeHtml(html: string) {
  return html
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

var movePattern = /\b(\d+)\s?(\.+)\s?(?:[o0-]+|[NBRQK]*[a-h]?[1-8]?x?@?[a-h][0-9]=?[NBRQK]?)\+?\#?[!\?=]*/gi;
function moveReplacer(match: string, turn: number, dots: string) {
  if (turn < 1 || turn > 200) return match;
  var ply = turn * 2 - (dots.length > 1 ? 0 : 1);
  return '<a class="jump" data-ply="' + ply + '">' + match + '</a>';
}

function addPlies(html: string) {
  return html.replace(movePattern, moveReplacer);
}

export default function enhance(text: string, parseMoves: boolean): string {
  var escaped = escapeHtml(delocalize(text));
  var linked = autoLink(escaped);
  var plied = (parseMoves && linked === escaped) ? addPlies(linked) : linked;
  return plied;
}
