var m = require('mithril');
var opposite = require('chessground').util.opposite;

function renderTitle(close) {
  return m('div.title', [
    m('span', 'Practice with the computer'),
    close ? m('span.close[data-icon=L]', {
      onclick: close
    }) : null
  ]);
}

var commentText = {
  good: 'Good move',
  inaccuracy: 'Inaccuracy',
  mistake: 'Mistake',
  blunder: 'Blunder'
};

var endText = {
  checkmate: 'Checkmate!',
  threefold: 'Threefold repetition',
  draw: 'Draw.'
};

function commentBest(c, ctrl) {
  if (!c.best) return;
  var pre = c.verdict === 'good' ? 'Another was' : 'Best was';
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

function renderEnd(root, end) {
  var color = root.turnColor();
  if (end === 'checkmate') color = opposite(color);
  return m('div.player', [
    color ? m('div.no-square', m('piece.king.' + color)) : m('div.icon.off', '!'),
    m('div.instruction', [
      m('strong', endText[end]),
      m('em', end === 'checkmate' ? [
        m('color', color),
        ' wins.'
      ] : 'The game is a draw.')
    ])
  ]);
}

var minDepth = 8;

function renderEvalProgress(root, maxDepth) {
  var node = root.vm.node;
  return m('div.progress', m('div', {
    style: {
      width: node.ceval ? (100 * Math.max(0, node.ceval.depth - minDepth) / (maxDepth - minDepth)) + '%' : 0
    }
  }));
}

function renderRunning(root) {
  var ctrl = root.practice;
  var hint = ctrl.hinting();
  return m('div.player', [
    m('div.no-square', m('piece.king.' + root.turnColor())),
    m('div.instruction', [
      ctrl.isMyTurn() ? m('strong', 'Your move') : [
        m('strong', 'Computer thinking...'),
        renderEvalProgress(root, ctrl.playableDepth())
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
  var end = root.vm.node.threefold ? 'threefold' : root.gameOver();
  return m('div', {
    class: 'practice_box ' + (comment ? comment.verdict : '')
  }, [
    renderTitle(root.studyPractice ? null : root.togglePractice),
    m('div.feedback', !running ? renderOffTrack(ctrl) : (end ? renderEnd(root, end) : renderRunning(root))),
    running ? m('div.comment', comment ? [
      m('span.verdict', commentText[comment.verdict]),
      ' ',
      commentBest(comment, ctrl)
    ] : (ctrl.isMyTurn() || end ? '' : m('span.wait', 'Evaluating your move...'))) : m('div.comment')
  ]);
};
