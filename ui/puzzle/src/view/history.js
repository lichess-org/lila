var m = require('mithril');

var historySize = 15;
var recentHash = '';

module.exports = function(ctrl) {
  var hash = ctrl.recentHash();
  if (hash === recentHash) return {subtree: 'retain'};
  recentHash = hash;
  var data = ctrl.getData();
  if (!data.user) return;
  var slots = [];
  for (var i = 0; i < historySize; i++) slots[i] = data.user.recent[i] || null;
  return m('div.history', [
    m('div.timeline', [
      slots.map(function(s) {
        if (s) return m('a', {
          class: data.puzzle.id === s[0] ? 'current' : (s[1] >= 0 ? 'win' : 'loss'),
          href: '/training/' + s[0]
        }, s[1] > 0 ? '+' + s[1] : s[1]);
        return m('span', ' ');
      })
    ])
  ]);
};
