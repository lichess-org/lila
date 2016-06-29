var m = require('mithril');

module.exports = {
  ctrl: function(opts) {
    var steps = opts.steps;
    var step = m.prop(opts.step || 1);
    return {
      steps: steps,
      step: step,
      inc: function() {
        step(step() + 1);
      }
    };
  },
  view: function(ctrl) {
    var steps = [];
    for (var i = 1; i <= ctrl.steps; i++) {
      var status = i === ctrl.step() ? 'active' : (i < ctrl.step() ? 'done' : 'future');
      var label = status === 'done' ? '✓' : (status === 'active' ? '+' : null);
      steps.push([
        i === 1 ? null : m('span', {
          class: 'bar ' + status
        }),
        m('span', {
          class: 'circle ' + status
        }, label ? m('span.label', label) : null)
      ]);
    }

    return m('div.progress', steps);
  }
};
