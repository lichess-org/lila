import { opposite } from 'chessground/util';
import { bind } from '../util';
import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { PracticeController, Comment } from './practiceCtrl';
import AnalyseController from '../ctrl';

function renderTitle(close: (() => void) | null): VNode {
  return h('div.title', [
    h('span', 'Practice with the computer'),
    close ? h('span.close', {
      attrs: { 'data-icon': 'L' },
      hook: bind('click', close)
    }) : null
  ]);
}

const commentText = {
  good: 'Good move',
  inaccuracy: 'Inaccuracy',
  mistake: 'Mistake',
  blunder: 'Blunder'
};

const endText = {
  checkmate: 'Checkmate!',
  threefold: 'Threefold repetition',
  draw: 'Draw.'
};

function commentBest(c: Comment, ctrl: PracticeController): Array<string | VNode> | undefined {
  if (c.best) return [
    c.verdict === 'good' ? 'Another was' : 'Best was',
    h('a', {
      hook: {
        insert: vnode => {
          const el = vnode.elm as HTMLElement;
          el.addEventListener('click', ctrl.playCommentBest);
          el.addEventListener('mouseover', () => ctrl.commentShape(true));
          el.addEventListener('mouseout', () => ctrl.commentShape(false));
        },
        destroy: () => ctrl.commentShape(false)
      }
    },
    c.best.san)
  ];
}

function renderOffTrack(ctrl: PracticeController): VNode {
  return h('div.player', [
    h('div.icon.off', '!'),
    h('div.instruction', [
      h('strong', 'You browsed away'),
      h('div.choices', [
        h('a', { hook: bind('click', ctrl.resume) }, 'Resume practice')
      ])
    ])
  ]);
}

function renderEnd(color: Color, end: string): VNode {
  if (end === 'checkmate') color = opposite(color);
  return h('div.player', [
    color ? h('div.no-square', h('piece.king.' + color)) : h('div.icon.off', '!'),
    h('div.instruction', [
      h('strong', endText[end]),
      h('em', end === 'checkmate' ? [
        h('color', color),
        ' wins.'
      ] : ['The game is a draw.'])
    ])
  ]);
}

const minDepth = 8;

function renderEvalProgress(node: Tree.Node, maxDepth: number) {
  return h('div.progress', h('div', {
    attrs: {
      style: `width: ${node.ceval ? (100 * Math.max(0, node.ceval.depth - minDepth) / (maxDepth - minDepth)) + '%' : 0}`
    }
  }));
}

function renderRunning(root: AnalyseController, ctrl: PracticeController): VNode {
  const hint = ctrl.hinting();
  return h('div.player', [
    h('div.no-square', h('piece.king.' + root.turnColor())),
    h('div.instruction', [
      ctrl.isMyTurn() ? h('strong', 'Your move') : [
        h('strong', 'Computer thinking...'),
        renderEvalProgress(ctrl.currentNode(), ctrl.playableDepth())
      ],
      h('div.choices', [
        ctrl.isMyTurn() ? h('a', {
          onclick: ctrl.hint
        }, hint ? (hint.mode === 'piece' ? 'See best move' : 'Hide best move') : 'Get a hint') : ''
      ])
    ])
  ]);
}

export default function(root: AnalyseController): VNode | undefined {
  const ctrl = root.practice;
  if (!ctrl) return;
  const comment: Comment = ctrl.comment();
  const running: boolean = ctrl.running();
  const end = ctrl.currentNode().threefold ? 'threefold' : root.gameOver();
  return h('div.practice_box', {
    class: { [comment.verdict]: !!comment }
  }, [
    renderTitle(root.studyPractice ? null : root.togglePractice),
    h('div.feedback', !running ? renderOffTrack(ctrl) : (end ? renderEnd(root.turnColor(), end) : renderRunning(root, ctrl))),
    running ? h('div.comment', comment ? [
      h('span.verdict', commentText[comment.verdict]),
      ' ',
      commentBest(comment, ctrl)
    ] : [ctrl.isMyTurn() || end ? '' : h('span.wait', 'Evaluating your move...')]) : (
      running ? h('div.comment') : null
    )
  ]);
};
