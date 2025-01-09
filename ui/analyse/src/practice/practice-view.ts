import { MaybeVNodes, bind } from 'common/snabbdom';
import { isHandicap } from 'shogiops/handicaps';
import { Outcome } from 'shogiops/types';
import { VNode, h } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { Comment, PracticeCtrl, Verdict } from './practice-ctrl';
import { i18n, i18nFormatCapitalized, i18nVdom } from 'i18n';
import { colorName } from 'shogi/color-name';

function commentBest(c: Comment, ctrl: PracticeCtrl): MaybeVNodes {
  return c.best
    ? i18nVdom(
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
          c.best.usi
        )
      )
    : [];
}

function renderOffTrack(ctrl: PracticeCtrl): VNode {
  return h('div.player.off', [
    h('div.icon.off', '!'),
    h('div.instruction', [
      h('strong', i18n('youBrowsedAway')),
      h('div.choices', [
        h('a', { hook: bind('click', ctrl.resume, ctrl.redraw) }, i18n('resumePractice')),
      ]),
    ]),
  ]);
}

function renderEnd(root: AnalyseCtrl, end: Outcome): VNode {
  const color = end.winner || root.turnColor();
  return h('div.player', [
    color ? h('div.no-square', h('piece.king.' + color)) : h('div.icon.off', '!'),
    h('div.instruction', [
      h('strong', end.winner ? i18n('checkmate') : i18n('draw')),
      end.winner
        ? h(
            'em',
            h(
              'color',
              i18nFormatCapitalized(
                'xWinsGame',
                colorName(
                  end.winner,
                  isHandicap({
                    rules: root.data.game.variant.key,
                    sfen: root.data.game.initialSfen,
                  })
                )
              )
            )
          )
        : h('em', i18n('theGameIsADraw')),
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
          node.ceval
            ? (100 * Math.max(0, node.ceval.depth - minDepth)) / (maxDepth - minDepth) + '%'
            : 0
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
        ? [h('strong', i18n('yourTurn'))]
        : [
            h('strong', i18n('computerThinking')),
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
                hint
                  ? hint.mode === 'piece'
                    ? i18n('seeBestMove')
                    : i18n('hideBestMove')
                  : i18n('getAHint')
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
  const end: Outcome | undefined = ctrl.currentNode().fourfold
    ? { result: 'draw', winner: undefined }
    : root.outcome();
  return h('div.practice-box.training-box.sub-box.' + (comment ? comment.verdict : 'no-verdict'), [
    h('div.title', i18n('practiceWithComputer')),
    h(
      'div.feedback',
      !running ? renderOffTrack(ctrl) : end ? renderEnd(root, end) : renderRunning(root, ctrl)
    ),
    running
      ? h(
          'div.comment',
          comment
            ? ([h('span.verdict', i18nVerdict(comment.verdict)), ' '] as MaybeVNodes).concat(
                commentBest(comment, ctrl)
              )
            : [ctrl.isMyTurn() || end ? '' : h('span.wait', i18n('evaluatingYourMove'))]
        )
      : running
        ? h('div.comment')
        : null,
  ]);
}

function i18nVerdict(v: Verdict): string {
  switch (v) {
    case 'blunder':
      return i18n('blunder');
    case 'goodMove':
      return i18n('goodMove');
    case 'inaccuracy':
      return i18n('inaccuracy');
    case 'mistake':
      return i18n('mistake');
    default:
      return '';
  }
}
