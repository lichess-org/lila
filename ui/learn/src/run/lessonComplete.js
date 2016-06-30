var m = require('mithril');
var scoring = require('../score');

function makeStars(rank) {
  var stars = [];
  for (var i = 3; i > 0; i--)
    stars.push(m('div.star-wrap', rank <= i ? m('i.star') : null));
  return stars;
}

module.exports = function(lesson, next) {
  return m('div.screen-overlay', {
      onclick: function(e) {
        if (e.target.classList.contains('screen-overlay')) m.route('/');
      }
    },
    m('div.screen', [
      m('div.stars', makeStars(scoring.getLevelRank(lesson.blueprint, lesson.vm.score))),
      m('h1', 'Level ' + lesson.blueprint.id + ' complete'),
      m('span.score', [
        'Your score: ',
        m('span', {
          config: function(el, isUpdate) {
            if (!isUpdate) setTimeout(function() {
              var score = lesson.vm.score;
              $.spreadNumber(el, 50, function() {
                return 3000;
              }, 0)(score);
            }, 300);
          }
        }, 0)
      ]),
      m('p', [
        m.trust(lesson.blueprint.complete)
      ]),
      m('div.buttons', [
        next ? m('a.next', {
          href: '/' + next.id,
          config: m.route
        }, [
          'Next: ',
          next.title + ' ',
          m('i[data-icon=H]')
        ]) : null,
        m('a.back.text[data-icon=I]', {
          href: '/',
          config: m.route
        }, 'Back to learning map')
      ])
    ])
  );
};
