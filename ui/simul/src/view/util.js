var m = require('mithril');

function playerHtml(p) {
  var html = '<a class="text ulpt user-link online" href="/@/' + p.username + '">';
  html += p.patron ? '<i class="line patron"></i>' : '<i class="line"></i>';
  html += (p.title ? p.title + ' ' : '') + p.username;
  if (p.rating) html += '<em>' + p.rating + (p.provisional ? '?' : '') + '</em>';
  html += '</a>';
  return html;
}


function enrichText(text) {
  return m.trust(autolink(lichess.escapeHtml(text), toLink).replace(newLineRegex, '<br>'));
}
function autolink(str, callback) {
  return str.replace(linkRegex, (_, space, url) => space + callback(url));
}
function toLink(url) {
  if (commentYoutubeRegex.test(url)) return toYouTubeEmbed(url) || url;
  const show = imageTag(url) || url.replace(/https?:\/\//, '');
  return '<a target="_blank" rel="nofollow" href="' + url + '">' + show + '</a>';
}
// from ui/analyse
const linkRegex = /(^|[\s\n]|<[A-Za-z]*\/?>)((?:https?|ftp):\/\/[\-A-Z0-9+\u0026\u2019@#\/%?=()~_|!:,.;]*[\-A-Z0-9+\u0026@#\/%=~()_|])/gi;
const newLineRegex = /\n/g;

module.exports = {
  title: function(ctrl) {
    return m('h1', [
      ctrl.data.fullName,
      m('span.author', m.trust(ctrl.trans('by', playerHtml(ctrl.data.host))))
    ]);
  },
  player: function(p) {
    return m.trust(playerHtml(p));
  },
  playerVariant: function(ctrl, p) {
    return ctrl.data.variants.find(function(v) {
      return v.key === p.variant;
    });
  },
  simulText: function(data) {
    return data.text ? m('div.simul-text', enrichText(data.text)) : null;
  }
};
