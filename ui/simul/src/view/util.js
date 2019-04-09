var m = require('mithril');

function playerHtml(p) {
  var html = '<a class="text ulpt user_link online" href="/@/' + p.username + '">';
  html += p.patron ? '<i class="line patron"></i>' : '<i class="line"></i>';
  html += (p.title ? p.title + ' ' : '') + p.username;
  if (p.rating) html += '<em>' + p.rating + (p.provisional ? '?' : '') + '</em>';
  html += '</a>';
  return html;
}

module.exports = {
  title: function(ctrl) {
    return m('div', [
      m('h1.text[data-icon=|]', [
        ctrl.data.fullName,
        m('span.author', m.trust(ctrl.trans('by', playerHtml(ctrl.data.host)))), m('br'),
        ctrl.data.arbiter ? m('span.arbiter', ctrl.trans('arbiter'), m.trust(playerHtml(ctrl.data.arbiter))) : null
      ]),
      ctrl.data.description ? m('span.description', m.trust(ctrl.data.description)) : null
    ]);
  },
  player: function(p) {
    return m.trust(playerHtml(p));
  },
  playerVariant: function(ctrl, p) {
    return ctrl.data.variants.find(function(v) {
      return v.key === p.variant;
    });
  }
};
