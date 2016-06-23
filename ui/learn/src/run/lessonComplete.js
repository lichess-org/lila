var m = require('mithril');

module.exports = function(lesson, next) {
  return m('div.screen-overlay',
    m('div.screen', [
      m('img.trophy', {
        src: "http://s22.postimg.org/tg9t79o0d/trophy.png",
      }),
      m('h1', 'Level ' + lesson.blueprint.id + ' complete'),
      m('span.score', [
        'Your score:',
        m('span.num', lesson.vm.score)
      ]),
      m('p', m.trust(lesson.blueprint.complete)),
      next ? m('div.buttons',
        m('a.light', {
          href: '/' + next.id,
          config: m.route
        }, 'Next level: ' + next.title)
      ) : null
    ])
  );
};
