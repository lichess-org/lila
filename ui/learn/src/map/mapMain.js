var m = require('mithril');
var stages = require('../stages');

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
        m('div.stages', stages.all.map(function(s) {
          return m('a', {
            href: '/' + s.id,
            config: m.route
          }, s.title);
        }))
      ]);
    }
  };
}
