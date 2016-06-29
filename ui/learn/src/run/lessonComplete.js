var m = require('mithril');

module.exports = function(lesson, next) {
  return m('div.screen-overlay', {
      onclick: function(e) {
        if (e.target.classList.contains('screen-overlay')) m.route('/');
      }
    },
    m('div.screen', [
      m('img.trophy', {
        src: "http://s22.postimg.org/tg9t79o0d/trophy.png",
      }),
      m('h1', 'Level ' + lesson.blueprint.id + ' complete'),
      m('span.score', [
        'Your score:',
        m('span.num', lesson.vm.score)
      ]),
      m('p', [
        m.trust(lesson.blueprint.complete)
      ]),
      m('div.buttons', [
        next ? m('a.light', {
          href: '/' + next.id,
          config: m.route
        }, [
          'Next level: ',
          next.title + ' ',
          m('i[data-icon=H]')
        ]) : null,
        m('a.dark.text[data-icon=I]', {
          href: '/',
          config: m.route
        }, 'Back to learning map')
      ])
    ])
  );
};
