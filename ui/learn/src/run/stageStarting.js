var m = require('mithril');

module.exports = function(stage, next) {
  return m('div.screen-overlay', {
      onclick: stage.start
    },
    m('div.screen', [
      m('h1', 'Stage ' + stage.blueprint.id + ': ' + stage.blueprint.title),
      stage.blueprint.illustration,
      m('p', m.trust(stage.blueprint.intro)),
      m('div.buttons',
        m('a.next', {
          onclick: stage.start
        }, "Let's go!")
      )
    ])
  );
};
