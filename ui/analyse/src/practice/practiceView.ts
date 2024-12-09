import type { Outcome } from 'chessops/types';
import { h, type VNode } from 'snabbdom';
import { bind, type MaybeVNodes } from 'common/snabbdom';
import type { PracticeCtrl, Comment } from './practiceCtrl';
import type AnalyseCtrl from '../ctrl';
import { renderNextChapter } from '../study/nextChapter';

function commentBest(c: Comment, ctrl: PracticeCtrl): MaybeVNodes {
  return c.best
    ? i18n.site[c.verdict === 'goodMove' ? 'anotherWasX' : 'bestWasX'].asArray(
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
          c.best.san,
        ),
      )
    : [];
}

function renderOffTrack(ctrl: PracticeCtrl): VNode {
  return h('div.player.off', [
    h('div.icon.off', '!'),
    h('div.instruction', [
      h('strong', i18n.site.youBrowsedAway),
      h('div.choices', [h('a', { hook: bind('click', ctrl.resume, ctrl.redraw) }, i18n.site.resumePractice)]),
    ]),
  ]);
}

function renderEnd(root: AnalyseCtrl, end: Outcome): VNode {
  const color = end.winner || root.turnColor();
  const isFiftyMoves = root.practice?.currentNode().fen.split(' ')[4] === '100';
  return h('div.player', [
    color ? h('div.no-square', h('piece.king.' + color)) : h('div.icon.off', '!'),
    h('div.instruction', [
      h('strong', end.winner ? i18n.site.checkmate : i18n.site.draw),
      end.winner
        ? h('em', h('color', i18n.site[end.winner === 'white' ? 'whiteWinsGame' : 'blackWinsGame']))
        : isFiftyMoves
          ? i18n.site.drawByFiftyMoves
          : h('em', i18n.site.theGameIsADraw),
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
    }),
  );
}

function renderRunning(root: AnalyseCtrl, ctrl: PracticeCtrl): VNode {
  const hint = ctrl.hinting();
  return h('div.player.running', [
    h('div.no-square', h('piece.king.' + root.turnColor())),
    h(
      'div.instruction',
      (ctrl.isMyTurn()
        ? [h('strong', i18n.site.yourTurn)]
        : [
            h('strong', i18n.site.computerThinking),
            renderEvalProgress(ctrl.currentNode(), ctrl.playableDepth()),
          ]
      ).concat(
        h('div.choices', [
          ctrl.isMyTurn()
            ? h(
                'a',
                { hook: bind('click', () => root.practice!.hint(), ctrl.redraw) },
                hint
                  ? hint.mode === 'piece'
                    ? i18n.site.seeBestMove
                    : i18n.site.hideBestMove
                  : i18n.site.getAHint,
              )
            : '',
        ]),
      ),
    ),
  ]);
}

export default function (root: AnalyseCtrl): VNode | undefined {
  const ctrl = root.practice;
  if (!ctrl) return;
  const comment: Comment | null = ctrl.comment();
  const isFiftyMoves = ctrl.currentNode().fen.split(' ')[4] === '100';
  const running: boolean = ctrl.running();
  const end = ctrl.currentNode().threefold || isFiftyMoves ? { winner: undefined } : root.outcome();
  return h('div.practice-box.training-box.sub-box.' + (comment ? comment.verdict : 'no-verdict'), [
    h('div.title', i18n.site.practiceWithComputer),
    h(
      'div.feedback',
      !running ? renderOffTrack(ctrl) : end ? renderEnd(root, end) : renderRunning(root, ctrl),
    ),
    running
      ? h(
          'div.comment',
          (end && !root.study?.practice ? renderNextChapter(root) : null) ||
            (comment
              ? (
                  [
                    h(
                      'span.verdict',
                      comment.verdict === 'goodMove' ? i18n.study.goodMove : i18n.site[comment!.verdict],
                    ),
                    ' ',
                  ] as MaybeVNodes
                ).concat(commentBest(comment, ctrl))
              : [ctrl.isMyTurn() || end ? '' : h('span.wait', i18n.site.evaluatingYourMove)]),
        )
      : null,
  ]);
}
