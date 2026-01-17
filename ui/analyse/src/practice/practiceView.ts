import type { Outcome } from 'chessops/types';
import { hl, type VNode, bind, type MaybeVNodes } from 'lib/view';
import type { PracticeCtrl, Comment } from './practiceCtrl';
import type AnalyseCtrl from '../ctrl';
import { renderNextChapter } from '../study/nextChapter';
import type { Prop } from 'lib';

function commentBest(c: Comment, ctrl: PracticeCtrl): MaybeVNodes {
  return c.best
    ? i18n.site[c.verdict === 'goodMove' ? 'anotherWasX' : 'bestWasX'].asArray(
        hl(
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
  return hl('div.player.off', [
    hl('div.icon.off', '!'),
    hl('div.instruction', [
      hl('strong', i18n.site.youBrowsedAway),
      hl('div.choices', [
        hl('a', { hook: bind('click', ctrl.resume, ctrl.redraw) }, i18n.site.resumePractice),
      ]),
    ]),
  ]);
}

function renderEnd(root: AnalyseCtrl, end: Outcome): VNode {
  const color = end.winner || root.turnColor();
  const isFiftyMoves = root.practice?.currentNode().fen.split(' ')[4] === '100';
  return hl('div.player', [
    color ? hl('div.no-square', hl('piece.king.' + color)) : hl('div.icon.off', '!'),
    hl('div.instruction', [
      hl('strong', end.winner ? i18n.site.checkmate : i18n.site.draw),
      end.winner
        ? hl('em', hl('color', i18n.site[end.winner === 'white' ? 'whiteWinsGame' : 'blackWinsGame']))
        : isFiftyMoves
          ? i18n.site.drawByFiftyMoves
          : hl('em', i18n.site.theGameIsADraw),
    ]),
  ]);
}

function renderRunning(root: AnalyseCtrl, ctrl: PracticeCtrl): VNode {
  const hint = ctrl.hinting();
  return hl('div.player.running', [
    hl('div.no-square', hl('piece.king.' + root.turnColor())),
    hl(
      'div.instruction',
      (ctrl.isMyTurn()
        ? [hl('strong', i18n.site.yourTurn)]
        : [hl('strong', i18n.site.computerThinking)]
      ).concat(
        hl('div.choices', [
          ctrl.isMyTurn()
            ? hl(
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

export function renderCustomPearl({ ceval }: AnalyseCtrl, hardMode: boolean): VNode {
  if (hardMode) {
    const time = i18n.site.nbSeconds(
      !isFinite(ceval.storedMovetime()) ? 60 : Math.round(ceval.storedMovetime() / 1000),
    );
    return hl('div.practice-mode', [hl('p', 'Mastery'), hl('p.secondary', time)]);
  }
  return hl('div.practice-mode', [hl('p', 'Casual'), hl('p.secondary', 'depth 18')]);
}

export function renderCustomStatus({ ceval }: AnalyseCtrl, hardMode: Prop<boolean>): VNode | undefined {
  return ceval.isComputing
    ? undefined
    : hl(
        'button.status.button-link',
        { hook: bind('click', () => hardMode(!hardMode())) },
        'Toggle difficulty',
      );
}

export default function (root: AnalyseCtrl): VNode | undefined {
  const ctrl = root.practice;
  if (!ctrl) return;
  const comment: Comment | null = ctrl.comment();
  const isFiftyMoves = ctrl.currentNode().fen.split(' ')[4] === '100';
  const running: boolean = ctrl.running();
  const end = ctrl.currentNode().threefold || isFiftyMoves ? { winner: undefined } : root.node.outcome();
  return hl('div.practice-box.training-box.sub-box.' + (comment ? comment.verdict : 'no-verdict'), [
    hl('div.title', i18n.site.practiceWithComputer),
    hl(
      'div.feedback',
      end ? renderEnd(root, end) : running ? renderRunning(root, ctrl) : renderOffTrack(ctrl),
    ),
    running
      ? hl(
          'div.comment',
          (end && !root.study?.practice ? renderNextChapter(root) : null) ||
            (comment
              ? (
                  [
                    hl(
                      'span.verdict',
                      comment.verdict === 'goodMove' ? i18n.study.goodMove : i18n.site[comment!.verdict],
                    ),
                    ' ',
                  ] as MaybeVNodes
                ).concat(commentBest(comment, ctrl))
              : [ctrl.isMyTurn() || end ? '' : hl('span.wait', i18n.site.evaluatingYourMove)]),
        )
      : null,
  ]);
}
