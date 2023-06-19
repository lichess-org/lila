import { bind, dataIcon } from 'common/snabbdom';
import { transWithColorName } from 'common/colorName';
import { toBlackWhite } from 'shogiops/util';
import { VNode, h } from 'snabbdom';
import { iconTag, richHTML } from '../../util';
import GamebookPlayCtrl, { Feedback, State } from './gamebookPlayCtrl';
import { isHandicap } from 'shogiops/handicaps';

const defaultComments: Record<Feedback, I18nKey> = {
  play: 'playQuestion',
  good: 'goodMove',
  bad: 'mistake',
  end: 'goldComplete', // why not
};

export function render(ctrl: GamebookPlayCtrl): VNode {
  const state = ctrl.state,
    comment = state.comment || ctrl.root.trans.noarg(defaultComments[state.feedback]);

  return h(
    'div.gamebook',
    {
      hook: {
        insert: _ => window.lishogi.loadCssPath('analyse.gamebook.play'),
      },
    },
    [
      comment
        ? h(
            'div.comment',
            {
              class: { hinted: state.showHint },
            },
            [h('div.content', { hook: richHTML(comment) }), hintZone(ctrl)]
          )
        : undefined,
      h('div.floor', [
        renderFeedback(ctrl, state),
        h('img.mascot', {
          attrs: {
            width: 120,
            height: 120,
            src: window.lishogi.assetUrl(`images/mascot/${mascot(ctrl)}.svg`),
          },
        }),
      ]),
    ]
  );
}

function mascot(ctrl: GamebookPlayCtrl) {
  switch (ctrl.root.data.game.variant.key) {
    case 'chushogi':
      return 'owl';
    case 'kyotoshogi':
      return 'camel-head';
    case 'minishogi':
      return 'parrot-head';
    default:
      return 'octopus';
  }
}

function hintZone(ctrl: GamebookPlayCtrl) {
  const state = ctrl.state,
    clickHook = () => ({
      hook: bind('click', ctrl.hint, ctrl.redraw),
    });
  if (state.showHint) return h('div', clickHook(), [h('div.hint', { hook: richHTML(state.hint!) })]);
  if (state.hint) return h('a.hint', clickHook(), ctrl.root.trans.noarg('getAHint'));
  return undefined;
}

function renderFeedback(ctrl: GamebookPlayCtrl, state: State) {
  const fb = state.feedback,
    color = ctrl.root.turnColor();
  if (fb === 'bad')
    return h(
      'div.feedback.act.bad' + (state.comment ? '.com' : ''),
      {
        hook: bind('click', ctrl.retry),
      },
      [iconTag('P'), h('span', ctrl.root.trans.noarg('retry'))]
    );
  if (fb === 'good' && state.comment)
    return h(
      'div.feedback.act.good.com',
      {
        hook: bind('click', ctrl.next),
      },
      [h('span.text', { attrs: dataIcon('G') }, 'Next'), h('kbd', '<space>')]
    );
  if (fb === 'end') return renderEnd(ctrl);
  return h(
    'div.feedback.info.' + fb + (state.init ? '.init' : ''),
    h(
      'div',
      fb === 'play'
        ? [
            h('img', {
              attrs: {
                width: 64,
                height: 64,
                src: window.lishogi.assetUrl(`images/${toBlackWhite(color)}Piece.svg`),
              },
            }),
            h('div.instruction', [
              h('strong', ctrl.trans.noarg('yourTurn')),
              h(
                'em',
                transWithColorName(
                  ctrl.trans,
                  'findTheBestMoveForX',
                  color,
                  isHandicap({ rules: ctrl.root.data.game.variant.key, sfen: ctrl.root.data.game.initialSfen })
                )
              ),
            ]),
          ]
        : [ctrl.trans.noarg('goodMove')]
    )
  );
}

function renderEnd(ctrl: GamebookPlayCtrl) {
  const study = ctrl.root.study!,
    nextChapter = study.nextChapter(),
    noarg = ctrl.root.trans.noarg;
  return h('div.feedback.end', [
    nextChapter
      ? h(
          'a.next.text',
          {
            attrs: dataIcon('G'),
            hook: bind('click', () => study.setChapter(nextChapter.id)),
          },
          noarg('next')
        )
      : undefined,
    h(
      'a.retry',
      {
        attrs: dataIcon('P'),
        hook: bind('click', () => ctrl.root.userJump(''), ctrl.redraw),
      },
      noarg('playAgain')
    ),
    h(
      'a.analyse',
      {
        attrs: dataIcon('A'),
        hook: bind('click', () => study.setGamebookOverride('analyse'), ctrl.redraw),
      },
      noarg('analyse')
    ),
  ]);
}
