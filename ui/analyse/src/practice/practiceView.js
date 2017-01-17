var m = require('mithril');

function renderTitle(ctrl) {
  return m('div.title', [
    m('span', 'Practice with the computer'),
    m('span.close[data-icon=L]', {
      onclick: ctrl.close
    })
  ]);
}

var commentText = {
  best: 'Best move!',
  good: 'Good move.',
  inaccuracy: 'Inaccuracy.',
  mistake: 'Mistake.',
  blunder: 'Blunder'
};

function commentBest(c) {
  if (c.prev.ceval.best === c.node.uci) return;
  var pre = c.verdict === 'best' ? 'An alternative was ' : 'Best was ';
  return pre + c.best.san;
}

function offTrack(ctrl) {
  return [
    m('div.player', [
      m('div.icon.off', '!'),
      m('div.instruction', [
        m('strong', 'You browsed away'),
        m('em', 'What do you want to do?'),
        m('div.choices', [
          m('a', {
            onclick: ctrl.resume
          }, 'Resume practice'),
          m('a', {
            onclick: ctrl.close
          }, 'Close practice')
        ])
      ])
    ])
  ];
}

module.exports = function(root) {
  var ctrl = root.practice;
  if (!ctrl) return;
  var comment = ctrl.comment();
  return m('div.practice_box', [
    renderTitle(ctrl),
    m('div.feedback', [
      ctrl.running() ? m('div.player', [
        m('div.no-square', m('piece.king.' + ctrl.turnColor())),
        m('div.instruction', [
          m('strong', ctrl.isMyTurn() ? 'Your move' : 'Computer thinking...'),
          m('div.choices', [
            m('a', {
              onclick: ctrl.hint
            }, 'Get a hint')
          ])
        ])
      ]) : offTrack(ctrl)
    ]),
    ctrl.running() ? m('div', {
      class: 'comment ' + (comment ? comment.verdict : 'none')
    }, comment ? [
      m('span', commentText[comment.verdict]),
      ' ',
      commentBest(comment)
    ] : (ctrl.isMyTurn() ? 'Think carefully...' : 'Evaluating your move...')) : null
  ]);
};
