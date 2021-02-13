import { Outcome } from 'chessops/types';
import { bind } from '../util';
import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { PracticeCtrl, Comment } from './practiceCtrl';
import AnalyseCtrl from '../ctrl';
import { MaybeVNodes } from '../interfaces';

function commentBest(c: Comment, root: AnalyseCtrl, ctrl: PracticeCtrl): MaybeVNodes {
  return c.best
    ? root.trans.vdom(
        c.verdict === 'goodMove' ? 'anotherWasX' : 'bestWasX',
        h(
          'move',
          {
            hook: {
              insert: vnode => {
                const el = vnode.elm as HTMLElement;
                el.addEventListener('click', ctrl.playCommentBest);
                el.addEventListener('mouseover', () => ctrl.commentShape(true));
                el.addEventListener('mouseout', () => ctrl.commentShape(false));
              },
              destroy: () => ctrl.commentShape(false),
            },
          },
          c.best.san
        )
      )
    : [];
}

function renderOffTrack(root: AnalyseCtrl, ctrl: PracticeCtrl): VNode {
  return h('div.player.off', [
    h('div.icon.off', '!'),
    h('div.instruction', [
      h('strong', root.trans.noarg('youBrowsedAway')),
      h('div.choices', [h('a', { hook: bind('click', ctrl.resume, ctrl.redraw) }, root.trans.noarg('resumePractice'))]),
    ]),
  ]);
}

function renderEnd(root: AnalyseCtrl, end: Outcome): VNode {
  const color = end.winner || root.turnColor();
  return h('div.player', [
    color ? h('div.no-square', h('piece.king.' + color)) : h('div.icon.off', '!'),
    h('div.instruction', [
      h('strong', root.trans.noarg(end.winner ? 'checkmate' : 'draw')),
      end.winner
        ? h('em', h('color', root.trans.noarg(end.winner === 'white' ? 'whiteWinsGame' : 'blackWinsGame')))
        : h('em', root.trans.noarg('theGameIsADraw')),
    ]),
  ]);
}

const minDepth = 8;

function renderEvalProgress(node: Tree.Node, maxDepth: number): VNode {
  return h(
    'div.progress',
    h('div', {
      attrs: {
        style: `width: ${
          node.ceval ? (100 * Math.max(0, node.ceval.depth - minDepth)) / (maxDepth - minDepth) + '%' : 0
        }`,
      },
    })
  );
}

function renderRunning(root: AnalyseCtrl, ctrl: PracticeCtrl): VNode {
  const hint = ctrl.hinting();
  return h('div.player.running', [
    h('div.no-square', h('piece.king.' + root.turnColor())),
    h(
      'div.instruction',
      (ctrl.isMyTurn()
        ? [h('strong', root.trans.noarg('yourTurn'))]
        : [
            h('strong', root.trans.noarg('computerThinking')),
            renderEvalProgress(ctrl.currentNode(), ctrl.playableDepth()),
          ]
      ).concat(
        h('div.choices', [
          ctrl.isMyTurn()
            ? h(
                'a',
                {
                  hook: bind('click', () => root.practice!.hint(), ctrl.redraw),
                },
                root.trans.noarg(hint ? (hint.mode === 'piece' ? 'seeBestMove' : 'hideBestMove') : 'getAHint')
              )
            : '',
        ])
      )
    ),
  ]);
}

export default function (root: AnalyseCtrl): VNode | undefined {
  const ctrl = root.practice;
  if (!ctrl) return;
  const comment: Comment | null = ctrl.comment();
  const running: boolean = ctrl.running();
  const end = ctrl.currentNode().threefold ? { winner: undefined } : root.outcome();
  return h('div.practice-box.training-box.sub-box.' + (comment ? comment.verdict : 'no-verdict'), [
    h('div.title', root.trans.noarg('practiceWithComputer')),
    h('div.feedback', !running ? renderOffTrack(root, ctrl) : end ? renderEnd(root, end) : renderRunning(root, ctrl)),
    running
      ? h(
          'div.comment',
          comment
            ? ([h('span.verdict', root.trans.noarg(comment.verdict)), ' '] as MaybeVNodes).concat(
                commentBest(comment, root, ctrl)
              )
            : [ctrl.isMyTurn() || end ? '' : h('span.wait', root.trans.noarg('evaluatingYourMove'))]
        )
      : running
      ? h('div.comment')
      : null,
  ]);
}
