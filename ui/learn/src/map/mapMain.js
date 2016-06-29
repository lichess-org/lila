var m = require('mithril');
var util = require('../util');
var lessons = require('../lesson/list');

module.exports = function(opts) {
  return {
    controller: function() {
      var data = opts.data;
      return {
        data: data
      };
    },
    view: function(ctrl) {
      var next = 1;
      lessons.list.forEach(function(l) {
        if (ctrl.data.levels[l.key]) next = l.id + 1;
      });
      return m('div.learn.map', [
        m('div.lessons', lessons.list.map(function(l) {
          var result = ctrl.data.levels[l.key];
          var status = result ? 'done' : (next === l.id ? 'next' : 'future')
          return m(status === 'future' ? 'span' : 'a', {
            class: 'lesson ' + status,
            href: '/' + l.id,
            config: status === 'future' ? null : m.route
          }, [
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
