var m = require('mithril');
var scoring = require('./score');

var star = m('i[data-icon=t]');

function makeStars(level, score) {
  var rank = scoring.getLevelRank(level, score);
  var stars = [];
  for (var i = 3; i >= rank; i--) stars.push(star);
  return m('span.stars.st' + stars.length, stars);
}

module.exports = {
  ctrl: function (stage, level, data) {
    return {
      stage: stage,
      level: level,
      score: function (level) {
        return data.stages[stage.key] ? data.stages[stage.key].scores[level.id - 1] : 0;
      },
    };
  },
  view: function (ctrl) {
    return m(
      'div.progress',
      ctrl.stage.levels.map(function (level) {
        var score = ctrl.score(level);
        var status = level.id === ctrl.level.blueprint.id ? 'active' : score ? 'done' : 'future';
        var label = score ? makeStars(level, score) : m('span.id', level.id);
        return m(
          'a',
          {
            href: '/' + ctrl.stage.id + '/' + level.id,
            config: m.route,
            class: status,
          },
          label
        );
      })
    );
  },
  makeStars: makeStars,
};
