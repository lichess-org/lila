var m = require('mithril');
var simul = require('../simul');
var xhrConfig = require('../xhr').xhrConfig;

function playerHtml(p, rating, provisional) {
  var onlineStatus = p.online === undefined ? 'online' : (p.online ? 'online' : 'offline');
  var html = '<a class="text ulpt user_link ' + onlineStatus + '" href="/@/' + p.username + '">';
  html += p.patron ? '<i class="line patron"></i>' : '<i class="line"></i>';
  html += (p.title ? ('<span class="title">' + p.title + '</span>') + ' ' : '') + p.username;
  if (rating === undefined) rating = p.rating;
  if (provisional === undefined) provisional = p.provisional;
  if (rating) html += '<em>' + rating + (provisional ? '?' : '') + '</em>';
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
      (ctrl.data.description && !ctrl.toggleArbiter) ? m('span.description', m.trust(ctrl.data.description)) : null
    ]);
  },
  player: function(p, r, pr) {
    return m.trust(playerHtml(p, r, pr));
  },
  playerVariant: function(ctrl, p) {
    return ctrl.data.variants.find(function(v) {
      return v.key === p.variant;
    });
  },
  arbiterOption: function(ctrl) {
    return simul.amArbiter(ctrl) ? m('div.top_right.option', {
      'data-icon': '%',
      'title': !ctrl.toggleArbiter ? 'Arbiter control panel' : 'Back to simul',
      onclick: function(e) {
        if (ctrl.toggleArbiter) ctrl.toggleArbiter = false;
        else m.request({
          method: 'GET',
          url: '/simul/' + ctrl.data.id + '/arbiter',
          config: xhrConfig
        }).then(function(d) {
          ctrl.arbiterData = d;
          ctrl.toggleArbiter = true;
        });
      }
    }) : null;
  }
};
