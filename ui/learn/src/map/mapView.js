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

function ribbon(s, status, res) {
  if (status === 'future') return;
  var rank = res ? scoring.getStageRank(s, res.scores) : null;
  var content = rank ? makeStars(rank) : 'play!';
  return m('span.ribbon-wrapper',
    m('span.ribbon', {
      class: status
    }, content)
  );
}

module.exports = function(ctrl) {
  return m('div.learn.map', [
    m('div.stages', stages.list.map(function(s) {
      var res = ctrl.data.stages[s.key];
      var previousDone = s.id === 1 ? true : !!ctrl.data.stages[stages.get(s.id - 1).key];
      var status = res ? 'done' : (previousDone ? 'next' : 'future')
      return m('a', {
        class: 'stage ' + status,
        href: '/' + s.id,
        config: m.route
      }, [
        ribbon(s, status, res),
        m('img', {
          src: s.image
        }),
        m('div.text', [
          m('h2', s.title),
          m('p.subtitle', s.subtitle)
        ])
      ]);
    }))
  ]);
};
