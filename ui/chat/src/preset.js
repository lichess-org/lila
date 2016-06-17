var m = require('mithril');

function splitIt(s) {
  return s.split('/');
};

var groups = {
  start: [
    'hi/Hello', 'gl/Good luck', 'hf/Have fun!', 'u2/You too!'
  ].map(splitIt),
  end: [
    'gg/Good game', 'wp/Well played', 'ty/Thank you', 'gtg/I\'ve got to go', 'bye/Bye!'
  ].map(splitIt)
};

module.exports = {
  ctrl: function(opts) {

    var group = m.prop(opts.initialGroup);

    var said = [];

    return {
      group: group,
      said: function() {
        return said;
      },
      setGroup: function(p) {
        if (p !== group()) {
          group(p);
          if (!p) said = [];
          m.redraw();
        }
      },
      post: function(preset) {
        var sets = groups[group()];
        if (!sets) return;
        if (said.indexOf(preset[0]) !== -1) return;
        opts.post(preset[1]);
        said.push(preset[0]);
      }
    };
  },
  view: function(ctrl) {
    var sets = groups[ctrl.group()];
    var said = ctrl.said();
    if (sets && said.length < 2) return m('div.presets', sets.map(function(p) {
      var disabled = said.indexOf(p[0]) !== -1;
      return m('span', {
        class: 'hint--top' + (disabled ? ' disabled' : ''),
        'data-hint': p[1],
        disabled: disabled,
        onclick: function() {
          if (!disabled) ctrl.post(p);
        }
      }, p[0]);
    }));
  }
};
