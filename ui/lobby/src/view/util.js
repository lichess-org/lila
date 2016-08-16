var m = require('mithril');

module.exports = {
  bindOnce: function(eventName, f) {
    var withRedraw = function(e) {
      m.startComputation();
      f(e);
      m.endComputation();
    };
    return function(el, isUpdate, ctx) {
      if (isUpdate) return;
      el.addEventListener(eventName, withRedraw)
      ctx.onunload = function() {
        el.removeEventListener(eventName, withRedraw);
      };
    }
  },
  tds: function(bits) {
    return bits.map(function(bit) {
      return {
        tag: 'td',
        children: [bit]
      };
    });
  }
};
