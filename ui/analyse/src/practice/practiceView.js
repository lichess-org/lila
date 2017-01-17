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
  var turnColor = ctrl.turnColor();
  var isMyTurn = root.bottomColor() === turnColor;
  return m('div.practice_box', [
    renderTitle(ctrl),
    m('div.feedback', [
      m('div.player', [
        m('div.no-square', m('piece.king.' + ctrl.turnColor())),
        m('div.instruction', [
          m('strong', isMyTurn ? 'Your move' : 'Computer thinking...'),
          m('div.choices', [
            m('a', {
              onclick: ctrl.hint
            }, 'Get a hint')
          ])
        ])
      ])
    ])
  ]);
};
