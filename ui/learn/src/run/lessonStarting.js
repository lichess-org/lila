var m = require('mithril');

module.exports = function(lesson, next) {
  return m('div.screen-overlay', {
      onclick: lesson.start
    },
    m('div.screen', [
      m('h1', 'Level ' + lesson.blueprint.id + ': ' + lesson.blueprint.title),
      lesson.blueprint.illustration,
      m('p', m.trust(lesson.blueprint.intro)),
      m('div.buttons',
        m('a.light', {
          onclick: lesson.start
        }, "Let's go!")
      )
    ])
  );
};
