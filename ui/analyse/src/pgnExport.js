var initialFen = require('./util').initialFen;

function renderStepsTxt(steps) {
  if (!steps[0]) return '';
  if (!steps[0].san) steps = steps.slice(1);
  if (!steps[0]) return '';
  var s = steps[0].ply % 2 === 1 ? '' : Math.floor((steps[0].ply + 1) / 2) + '... ';
  steps.forEach(function(step, i) {
    if (step.ply === 0) return;
    if (step.ply % 2 === 1) s += ((step.ply + 1) / 2) + '. '
    else s += '';
    s += step.san + ((i + 9) % 8 === 0 ? '\n' : ' ');
  });
  return s.trim();
}

module.exports = {
  renderFullTxt: function(ctrl) {
    var g = ctrl.data.game;
    var txt = renderStepsTxt(ctrl.analyse.getSteps(ctrl.vm.path));
    var tags = [];
    if (g.variant.key !== 'standard')
      tags.push(['Variant', g.variant.name]);
    if (g.initialFen && g.initialFen !== initialFen)
      tags.push(['FEN', g.initialFen]);
    if (tags.length)
      txt = tags.map(function(t) {
        return '[' + t[0] + ' "' + t[1] + '"]';
      }).join('\n') + '\n\n' + txt;
    return txt;
  },
  renderStepsHtml: function(steps) {
    if (!steps[0]) return '';
    if (!steps[0].san) steps = steps.slice(1);
    if (!steps[0]) return '';
    var s = steps[0].ply % 2 === 1 ? '' : Math.floor((steps[0].ply + 1) / 2) + '...&nbsp;';
    steps.forEach(function(step) {
      if (step.ply === 0) return;
      if (step.ply % 2 === 1) s += ((step.ply + 1) / 2) + '.&nbsp;'
      else s += '';
      s += '<san>' + step.san + '</san> ';
    });
    return s.trim();
  }
};
