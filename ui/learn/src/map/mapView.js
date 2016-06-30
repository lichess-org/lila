var m = require('mithril');
var util = require('../util');
var scoring = require('../score');
var lessons = require('../lesson/list');

function makeStars(nb) {
  var stars = [];
  for (var i = 0; i < nb; i++)
    stars.push(m('i', {'data-icon': 't'}));
  return stars;
}

function ribbon(l, status, result) {
  if (status === 'future') return;
  var rank = result ? scoring.getLevelRank(l, result.score) : null;
  var content = rank ? makeStars(rank) : 'play!';
  return m('div.ribbon-wrapper',
    m('div.ribbon', {
      class: status
    }, content)
  );
}

module.exports = function(ctrl) {
  return m('div.learn.map', [
    m('div.lessons', lessons.list.map(function(l) {
      var result = ctrl.data.levels[l.key];
      var previousDone = l.id === 1 ? true : !!ctrl.data.levels[lessons.get(l.id - 1).key];
      var status = result ? 'done' : (previousDone ? 'next' : 'future')
      return m(status === 'future' ? 'span' : 'a', {
        class: 'lesson ' + status,
        href: '/' + l.id,
        config: status === 'future' ? null : m.route
      }, [
        ribbon(l, status, result),
        m('img', {
          src: status === 'future' ? util.assetUrl + 'images/learn/help.svg' : l.image
        }),
        m('div.text', [
          m('h2', l.title),
          m('p.subtitle', l.subtitle)
        ])
      ]);
    }))
  ]);
};
