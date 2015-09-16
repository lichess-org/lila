module.exports = {
  renderStepsTxt: function(steps) {
    if (!steps[0]) return '';
    if (!steps[0].san) steps = steps.slice(1);
    if (!steps[0]) return '';
    var s = steps[0].ply % 2 === 1 ? '' : Math.floor((steps[0].ply + 1) / 2) + '... ';
    steps.forEach(function(step) {
      if (step.ply === 0) return;
      if (step.ply % 2 === 1) s += ((step.ply + 1) / 2) + '. '
      else s += '';
      s += step.san + ' ';
    });
    return s.trim();
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
