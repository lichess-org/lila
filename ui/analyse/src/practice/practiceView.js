var m = require('mithril');

function renderTitle(ctrl) {
  return m('div.title', [
    m('span', 'Practice with the computer'),
    m('span.close[data-icon=L]', {
      onclick: ctrl.close
    })
  ]);
}

module.exports = function(root) {
  var ctrl = root.practice;
  if (!ctrl) return;
  return m('div.practice_box', [
    renderTitle(ctrl),
    'Practice'
  ]);
};
