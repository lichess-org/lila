var m = require('mithril');

function enrichText(text) {
  return m.trust(autolink(lishogi.escapeHtml(text), toLink).replace(newLineRegex, '<br>'));
}
function autolink(str, callback) {
  return str.replace(linkRegex, function (_, space, url) {
    return space + callback(url);
  });
}
function toLink(url) {
  return (
    '<a target="_blank" rel="nofollow noopener noreferrer" href="' +
    url +
    '">' +
    url.replace(/https?:\/\//, '') +
    '</a>'
  );
}
// from ui/analyse
var linkRegex =
  /(^|[\s\n]|<[A-Za-z]*\/?>)((?:https?|ftp):\/\/[-A-Z0-9+\u0026\u2019@#/%?=()~_|!:,.;]*[-A-Z0-9+\u0026@#/%=~()_|])/gi;
var newLineRegex = /\n/g;

module.exports = {
  view: function (ctrl) {
    return ctrl.data.text ? m('div.simul-text', m('p', enrichText(ctrl.data.text))) : null;
  },
};
