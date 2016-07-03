var m = require('mithril');
var util = require('../util');
var stages = require('../stage/list');

module.exports = function(opts) {
  return {
    controller: function() {
      return {
        data: opts.storage.data,
        current: opts.stageId,
        enabled: function() {
          return opts.route === 'run';
        }
      };
    },
    view: function(ctrl) {
      if (ctrl.enabled()) return m('div.learn.map', [
        m('div.stages', [
          m('a.stage.home', {
            href: '/',
            config: m.route
          }, [
            m('i[data-icon=I]'),
            'Learn chess'
          ]),
          stages.list.map(function(s) {
            var result = ctrl.data.stages[s.key];
            var previousDone = s.id === 1 ? true : !!ctrl.data.stages[stages.get(s.id - 1).key];
            var status = result ? 'done' : (previousDone ? 'next' : 'future')
            var current = s.id === ctrl.current;
            return m('a', {
              class: 'stage ' + status + (current ? ' current' : ''),
              href: '/' + s.id,
              config: m.route
            }, [
              m('img', {
                src: s.image
              }),
              m('h2', s.title)
            ]);
          })
        ])
      ]);
    }
  };
}
