var m = require('mithril');

var linkPattern = /(^|[\s\n]|<[A-Za-z]*\/?>)((?:(?:https?):\/\/|lichess\.org\/)[\-A-Z0-9+\u0026\u2019@#\/%?=()~_|!:,.;]*[\-A-Z0-9+\u0026@#\/%=~()_|])/gi;

function linkReplace(match, before, url) {
  var fullUrl = url.indexOf('http') === 0 ? url : 'https://' + url;
  var minUrl = url.replace(/^(?:https:\/\/)?(.+)$/, '$1');
  return before + '<a target="_blank" rel="nofollow" href="' + fullUrl + '">' + minUrl + '</a>';
}

function autoLink(html) {
  return html.replace(linkPattern, linkReplace);
}

var delocalizePattern = /(^|[\s\n]|<[A-Za-z]*\/?>)\w{2}\.lichess\.org/gi;

function delocalize(html) {
  return html.replace(delocalizePattern, '$1lichess.org');
}

function escapeHtml(html) {
  return html
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

var movePattern = /\b(\d+)\s?(\.+)\s?(?:[o0-]+|(?:N|B|R|Q|K|)[a-h]?[1-8]?x?@?[a-h][0-9]=?[NBRQ]?)\+?\#?[!\?=]*/gi;
function moveReplacer(match, turn, dots) {
  var ply = turn * 2 - (dots.length > 1 ? 0 : 1);
  return '<a class="jump" data-ply="' + ply + '">' + match + '</a>';
}

function addPlies(html) {
  return html.replace(movePattern, moveReplacer);
}

module.exports = function(text, opts) {
  var escaped = escapeHtml(delocalize(text));
  var linked = autoLink(escaped);
  var plied = (opts.parseMoves && linked === escaped) ? addPlies(linked) : linked;
  return m.trust(plied);
};
