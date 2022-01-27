import { h, VNode } from 'snabbdom';
import GamebookPlayCtrl from './gamebookPlayCtrl';
import { bind, dataIcon } from 'common/snabbdom';
import { iconTag, richHTML } from '../../util';
// eslint-disable-next-line no-duplicate-imports
import { State } from './gamebookPlayCtrl';

export function render(ctrl: GamebookPlayCtrl): VNode {
  const state = ctrl.state;
  return h(
    'div.gamebook',
    {
      hook: { insert: _ => lichess.loadCssPath('analyse.gamebook.play') },
    },
    [
      state.comment || state.feedback == 'play' || state.feedback == 'end'
        ? h(
            'div.comment',
            {
              class: { hinted: state.showHint },
            },
            [
              state.comment
                ? h('div.content', { hook: richHTML(state.comment) })
                : h(
                    'div.content',
                    state.feedback == 'play'
                      ? ctrl.trans('whatWouldYouPlay')
                      : state.feedback == 'end'
                      ? ctrl.trans('youCompletedThisLesson')
                      : undefined
                  ),
              hintZone(ctrl),
            ]
          )
        : undefined,
      h('div.floor', [
        renderFeedback(ctrl, state),
        h('img.mascot', {
          attrs: {
            width: 120,
            height: 120,
            src: lichess.assetUrl('images/mascot/octopus.svg'),
          },
        }),
      ]),
    ]
  );
}

function hintZone(ctrl: GamebookPlayCtrl) {
  const state = ctrl.state,
    buttonData = () => ({
      attrs: { type: 'button' },
      hook: bind('click', ctrl.hint, ctrl.redraw),
    });
  if (state.showHint) return h('button', buttonData(), [h('div.hint', { hook: richHTML(state.hint!) })]);
  if (state.hint) return h('button.hint', buttonData(), ctrl.trans.noarg('getAHint'));
  return undefined;
}

function renderFeedback(ctrl: GamebookPlayCtrl, state: State) {
  const fb = state.feedback,
    color = ctrl.root.turnColor();
  if (fb === 'bad')
    return h(
      'button.feedback.act.bad' + (state.comment ? '.com' : ''),
      {
        attrs: { type: 'button' },
        hook: bind('click', ctrl.retry),
      },
      [iconTag(''), h('span', ctrl.trans.noarg('retry'))]
    );
  if (fb === 'good' && state.comment)
    return h(
      'button.feedback.act.good.com',
      {
        attrs: { type: 'button' },
        hook: bind('click', ctrl.next),
      },
      [h('span.text', { attrs: dataIcon('') }, ctrl.trans.noarg('next')), h('kbd', '<space>')]
    );
  if (fb === 'end') return renderEnd(ctrl);
  return h(
    'div.feedback.info.' + fb + (state.init ? '.init' : ''),
    h(
      'div',
      fb === 'play'
        ? [
            h('div.no-square', h('piece.king.' + color)),
            h('div.instruction', [
              h('strong', ctrl.trans.noarg('yourTurn')),
              h('em', ctrl.trans.noarg(color === 'white' ? 'findTheBestMoveForWhite' : 'findTheBestMoveForBlack')),
            ]),
          ]
        : ctrl.trans.noarg('goodMove')
    )
  );
}

function renderEnd(ctrl: GamebookPlayCtrl) {
  const study = ctrl.root.study!;
  return h('div.feedback.end', [
    study.nextChapter()
      ? h(
          'button.next.text',
          {
            attrs: {
              'data-icon': '',
              type: 'button',
            },
            hook: bind('click', study.goToNextChapter),
          },
          study.trans.noarg('nextChapter')
        )
      : undefined,
    h(
      'button.retry',
      {
        attrs: {
          'data-icon': '',
          type: 'button',
        },
        hook: bind('click', () => ctrl.root.userJump(''), ctrl.redraw),
      },
      study.trans.noarg('playAgain')
    ),
    h(
      'button.analyse',
      {
        attrs: {
          'data-icon': '',
          type: 'button',
        },
        hook: bind('click', () => study.setGamebookOverride('analyse'), ctrl.redraw),
      },
      study.trans.noarg('analysis')
    ),
  ]);
}
