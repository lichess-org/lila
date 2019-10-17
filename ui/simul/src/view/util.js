var m = require('mithril');
var simul = require('../simul');
var xhr = require('../xhr');

function playerHtml(p, rating, provisional, fmjd, href) {
  var onlineStatus = p.online === undefined ? 'online' : (p.online ? 'online' : 'offline');
  var html = '<a class="text ulpt user-link ' + onlineStatus;
  if (href)
    html += '" href="' + href + '" target="_blank">';
  else
    html += '" href="/@/' + p.name + '">';
  html += p.patron ? '<i class="line patron"></i>' : '<i class="line"></i>';
  html += (p.title ? ('<span class="title">' + p.title + '</span>') + ' ' : '') + p.name;
  if (fmjd) {
    html += '<em>' + fmjd + '</em> FMJD';
  } else {
    if (rating === undefined) rating = p.rating;
    if (provisional === undefined) provisional = p.provisional;
    if (rating) html += '<em>' + rating + (provisional ? '?' : '') + '</em>';
  }
  html += '</a>';
  return html;
}

module.exports = {
  title: function(ctrl) {
    return m('div', [
      m('h1', [
        ctrl.data.fullName,
        m('span.author', m.trust(ctrl.trans('by', playerHtml(ctrl.data.host, ctrl.data.host.rating, ctrl.data.host.provisional, ctrl.data.host.officialRating)))), m('br'),
        (ctrl.data.arbiter && !ctrl.data.arbiter.hidden) ? m('span.arbiter', ctrl.trans.noarg('arbiter'), m.trust(playerHtml(ctrl.data.arbiter))) : null
      ])
    ]);
  },
  player: function(p, r, pr, fmjd, href) {
    return m.trust(playerHtml(p, r, pr, (p && fmjd === undefined) ? p.officialRating : fmjd, href));
  },
  playerVariant: function(ctrl, p) {
    return ctrl.data.variants.find(function(v) {
      return v.key === p.variant;
    });
  },
  exportGames: function(ctrl) {
    return m('a', {
      'data-icon': 'x',
      'href': '/simul/' + ctrl.data.id + '/export',
      'title': ctrl.trans.noarg('exportSimulGames')
    });
  },
  hostTv: function(ctrl) {
    return m('a', {
      'data-icon': '1',
      'href': '/@/' + ctrl.data.host.id + '/tv',
      'title': ctrl.trans.noarg('followSimulHostTv')
    });
  },
  arbiterOption: function(ctrl) {
    return simul.amArbiter(ctrl) ? m('a', {
      'data-icon': '%',
      'title': !ctrl.toggleArbiter ? 'Arbiter control panel' : ctrl.trans.noarg('backToSimul'),
      onclick: function(e) {
        if (ctrl.toggleArbiter) {
          clearInterval(ctrl.arbiterInterval);
          ctrl.toggleArbiter = false;
        } else {
          xhr.arbiterData(ctrl);
          if (ctrl.data.isFinished) clearInterval(ctrl.arbiterInterval);
          else ctrl.arbiterInterval = setInterval(function() {
            xhr.arbiterData(ctrl);
          }, 2000);
        }
      }
    }) : null;
  }
};
