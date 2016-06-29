var m = require('mithril');
var util = require('../util');
var lessons = require('../lesson/list');

function ribbon(l, status, result) {
  if (status === 'future') return;
  var text = result ? result.score : 'play!';
  return m('div.ribbon-wrapper',
    m('div.ribbon', {
      class: status
    }, text)
  );
}

module.exports = function(opts) {
  return {
    controller: function() {
      var data = opts.data;
      return {
        data: data
      };
    },
    view: function(ctrl) {
      return m('div.learn.map', [
        m('div.lessons', lessons.list.map(function(l) {
          var result = ctrl.data.levels[l.key];
          var previousDone = l.id === 1 ? true : !!ctrl.data.levels[lessons.get(l.id - 1).key];
          var status = result ? 'done' : (previousDone ? 'next' : 'future')
          return m(status === 'future' ? 'span' : 'a', {
            class: 'lesson ' + status,
            href: '/' + l.id,
            config: status === 'future' ? null : m.route
          }, [
            ribbon(l, status, result),
            m('img', {
              src: status === 'future' ? util.assetUrl + 'images/learn/help.svg' : l.image
            }),
            m('div.text', [
              m('h2', l.title),
              m('p.subtitle', l.subtitle)
            ])
          ]);
        }))
      ]);
    }
  };
}
