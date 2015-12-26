var m = require('mithril');

module.exports = function(element, opts) {

  m.module(element, {
    controller: function() {
      return {
        data: opts.data
      };
    },
    view: require('./view')
  });
};
