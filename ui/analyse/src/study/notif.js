var m = require('mithril');

module.exports = {
  ctrl: function() {
    var current = null;
    var timeout = null;

    var set = function(opts) {
      clearTimeout(timeout);
      current = opts;
      timeout = setTimeout(function() {
        current = null;
        m.redraw();
      }, opts.duration);
    };
    return {
      set: set,
      get: function() {
        return current;
      }
    };
  },
  view: function(ctrl) {
    var c = ctrl.get();
    return c && m('div.notif.' + c.class, c.text);
  }
};
