var m = require('mithril');
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
      return m('div.learn.map', [
        m('h1', 'Learn map'),
        m('div.lessons', lessons.list.map(function(l) {
          return m('a', {
            href: '/' + l.id,
            config: m.route
          }, l.title);
        }))
      ]);
    }
  };
}
