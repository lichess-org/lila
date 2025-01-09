import { bind, dataIcon } from 'common/snabbdom';
import { richHTML } from 'common/rich-text';
import { toBlackWhite } from 'shogiops/util';
import { VNode, h } from 'snabbdom';
import { iconTag } from '../../util';
import GamebookPlayCtrl, { Feedback, State } from './gamebook-play-ctrl';
import { isHandicap } from 'shogiops/handicaps';
import { assetUrl, loadCssPath } from 'common/assets';
import { colorName } from 'shogi/color-name';
import { i18n, i18nFormatCapitalized } from 'i18n';

const defaultComments: Record<Feedback, string> = {
  play: i18n('study:playQuestion'),
  good: i18n('goodMove'),
  bad: i18n('mistake'),
  end: i18n('learn:goldComplete'), // why not
};

export function render(ctrl: GamebookPlayCtrl): VNode {
  const state = ctrl.state,
    comment = state.comment || defaultComments[state.feedback];

  return h(
    'div.gamebook',
    {
      hook: {
        insert: _ => loadCssPath('analyse.gamebook.play'),
      },
    },
    [
      comment
        ? h(
            'div.comment',
            {
              class: { hinted: state.showHint },
            },
            [h('div.content', { hook: richHTML(comment) }), hintZone(ctrl)],
          )
        : undefined,
      h('div.floor', [
        renderFeedback(ctrl, state),
        h('img.mascot', {
          attrs: {
            width: 120,
            height: 120,
            src: assetUrl(`images/mascot/${mascot(ctrl)}.svg`),
          },
        }),
      ]),
    ],
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
  if (state.showHint)
    return h('div', clickHook(), [h('div.hint', { hook: richHTML(state.hint!) })]);
  if (state.hint) return h('a.hint', clickHook(), i18n('getAHint'));
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
      [iconTag('P'), h('span', i18n('retry'))],
    );
  if (fb === 'good' && state.comment)
    return h(
      'div.feedback.act.good.com',
      {
        hook: bind('click', ctrl.next),
      },
      [h('span.text', { attrs: dataIcon('G') }, 'Next'), h('kbd', '<space>')],
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
                src: assetUrl(`images/${toBlackWhite(color)}Piece.svg`),
              },
            }),
            h('div.instruction', [
              h('strong', i18n('yourTurn')),
              h(
                'em',
                i18nFormatCapitalized(
                  'puzzle:findTheBestMoveForX',
                  colorName(
                    color,
                    isHandicap({
                      rules: ctrl.root.data.game.variant.key,
                      sfen: ctrl.root.data.game.initialSfen,
                    }),
                  ),
                ),
              ),
            ]),
          ]
        : [i18n('goodMove')],
    ),
  );
}

function renderEnd(ctrl: GamebookPlayCtrl) {
  const study = ctrl.root.study!,
    nextChapter = study.nextChapter();
  return h('div.feedback.end', [
    nextChapter
      ? h(
          'a.next.text',
          {
            attrs: dataIcon('G'),
            hook: bind('click', () => study.setChapter(nextChapter.id)),
          },
          i18n('next'),
        )
      : undefined,
    h(
      'a.retry',
      {
        attrs: dataIcon('P'),
        hook: bind('click', () => ctrl.root.userJump(''), ctrl.redraw),
      },
      i18n('storm:playAgain'),
    ),
    h(
      'a.analyse',
      {
        attrs: dataIcon('A'),
        hook: bind('click', () => study.setGamebookOverride('analyse'), ctrl.redraw),
      },
      i18n('analyse'),
    ),
  ]);
}
