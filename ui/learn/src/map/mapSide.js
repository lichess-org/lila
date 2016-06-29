var m = require('mithril');
var util = require('../util');
var lessons = require('../lesson/list');

module.exports = function(opts) {
  return {
    controller: function() {
      return {
        data: opts.data,
        current: opts.lessonId
      };
    },
    view: function(ctrl) {
      return m('div.learn.map', [
        m('div.lessons', [
          m('a.lesson.done', {
            href: '/',
            config: m.route
          }, [
            m('i[data-icon=I]'),
            'Learn chess'
          ]),
          lessons.list.map(function(l) {
            var result = ctrl.data.levels[l.key];
            var previousDone = l.id === 1 ? true : !!ctrl.data.levels[lessons.get(l.id - 1).key];
            var status = result ? 'done' : (previousDone ? 'next' : 'future')
            var current = l.id === ctrl.current;
            return m(status === 'future' ? 'span' : 'a', {
              class: 'lesson ' + status + (current ? ' current' : ''),
              href: '/' + l.id,
              config: status === 'future' ? null : m.route
            }, [
              m('img', {
                src: status === 'future' ? util.assetUrl + 'images/learn/help.svg' : l.image
              }),
              m('h2', l.title)
            ]);
          })
        ])
      ]);
    }
  };
}
