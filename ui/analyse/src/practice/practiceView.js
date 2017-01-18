var m = require('mithril');

function renderTitle(close) {
  return m('div.title', [
    m('span', 'Practice with the computer'),
    m('span.close[data-icon=L]', {
      onclick: close
    })
  ]);
}

var commentText = {
  best: 'Great move!',
  good: 'Good move.',
  inaccuracy: 'Inaccuracy.',
  mistake: 'Mistake.',
  blunder: 'Blunder'
};

var endText = {
  checkmate: 'Checkmate!',
  stalemate: 'Stalemate.'
};

var altCastles = {
  e1a1: 'e1c1',
  e1h1: 'e1g1',
  e8a8: 'e8c8',
  e8h8: 'e8g8'
};

function commentBest(c, ctrl) {
  if (c.prev.ceval.best === c.node.uci || c.prev.ceval.best === altCastles[c.node.uci]) return;
  var pre = c.verdict === 'best' ? 'An alternative was' : 'Best was';
  return [
    pre,
    m('a', {
        onclick: ctrl.playCommentBest,
        onmouseover: function() {
          ctrl.commentShape(true);
        },
        onmouseout: function() {
          ctrl.commentShape(false);
        },
        config: function(el, isUpdate, ctx) {
          if (!isUpdate) ctx.onunload = function() {
            ctrl.commentShape(false);
          };
        }
      },
      c.best.san)
  ];
}

function renderOffTrack(ctrl) {
  return [
    m('div.player', [
      m('div.icon.off', '!'),
      m('div.instruction', [
        m('strong', 'You browsed away'),
        m('div.choices', [
          m('a', {
            onclick: ctrl.resume
          }, 'Resume practice')
        ])
      ])
    ])
  ];
}

function renderEnd(ctrl, end) {
  return m('div.player', [
    ctrl.turnColor() ? m('div.no-square', m('piece.king.' + ctrl.turnColor())) : m('div.icon.off', '!'),
    m('div.instruction', [
      m('strong', endText[end]),
      m('em', end === 'checkmate' ? [
        m('color', ctrl.turnColor()),
        ' wins.'
      ] : 'the game is a draw.')
    ])
  ]);
}

var minDepth = 8;
var maxDepth = 18;

function renderEvalProgress(root) {
  var node = root.vm.node;
  return m('div.progress', node.ceval ? m('div', {
    style: {
      width: (100 * (node.ceval.depth - minDepth) / (maxDepth - minDepth)) + '%'
    }
  }) : null);
}

function renderRunning(root) {
  var ctrl = root.practice;
  var hint = ctrl.hinting();
  return m('div.player', [
    m('div.no-square', m('piece.king.' + ctrl.turnColor())),
    m('div.instruction', [
      ctrl.isMyTurn() ? m('strong', 'Your move') : [
        m('strong', 'Computer thinking...'),
        renderEvalProgress(root)
      ],
      m('div.choices', [
        ctrl.isMyTurn() ? m('a', {
          onclick: ctrl.hint
        }, hint ? (hint.mode === 'piece' ? 'See best move' : 'Hide best move') : 'Get a hint') : ''
      ])
    ])
  ]);
}

module.exports = function(root) {
  var ctrl = root.practice;
  if (!ctrl) return;
  var comment = ctrl.comment();
  var running = ctrl.running();
  var end = root.gameOver();
  return m('div.practice_box', [
    renderTitle(root.togglePractice),
    m('div.feedback', !running ? renderOffTrack(ctrl) : (end ? renderEnd(ctrl, end) : renderRunning(root))),
    ctrl.running() ? m('div', {
      class: 'comment ' + (comment ? comment.verdict : 'none')
    }, comment ? [
      m('span', commentText[comment.verdict]),
      ' ',
      commentBest(comment, ctrl)
    ] : (ctrl.isMyTurn() || end ? '' : 'Evaluating your move...')) : null
  ]);
};
