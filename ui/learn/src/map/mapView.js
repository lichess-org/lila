var m = require('mithril');
var util = require('../util');
var scoring = require('../score');
var stages = require('../stage/list');

function makeStars(nb) {
  var stars = [];
  for (var i = 0; i < 4 - nb; i++)
    stars.push(m('i', {
      'data-icon': 't'
    }));
  return stars;
}

function ribbon(s, status, result) {
  if (status === 'future') return;
  var rank = result ? scoring.getStageRank(s, result.score) : null;
  var content = rank ? makeStars(rank) : 'play!';
  return m('div.ribbon-wrapper',
    m('div.ribbon', {
      class: status
    }, content)
  );
}

module.exports = function(ctrl) {
  return m('div.learn.map', [
    m('div.stages', stages.list.map(function(s) {
      var result = ctrl.data.stages[s.key];
      var previousDone = s.id === 1 ? true : !!ctrl.data.stages[stages.get(s.id - 1).key];
      var status = result ? 'done' : (previousDone ? 'next' : 'future')
      return m(status === 'future' ? 'span' : 'a', {
        class: 'stage ' + status,
        href: '/' + s.id,
        config: status === 'future' ? null : m.route
      }, [
        ribbon(s, status, result),
        m('img', {
          src: status === 'future' ? util.assetUrl + 'images/learn/help.svg' : s.image
        }),
        m('div.text', [
          m('h2', s.title),
          m('p.subtitle', s.subtitle)
        ])
      ]);
    }))
  ]);
};
