import GamebookPlayCtrl, { type State } from './gamebookPlayCtrl';
import * as licon from 'common/licon';
import { type VNode, iconTag, bind, dataIcon, looseH as h } from 'common/snabbdom';
import { richHTML } from 'common/richText';

export function render(ctrl: GamebookPlayCtrl): VNode {
  const state = ctrl.state;
  return h('div.gamebook', { hook: { insert: _ => site.asset.loadCssPath('analyse.gamebook.play') } }, [
    (state.comment || state.feedback === 'play' || state.feedback === 'end') &&
      h('div.comment', { class: { hinted: state.showHint } }, [
        state.comment
          ? h('div.content', { hook: richHTML(state.comment) })
          : h(
              'div.content',
              state.feedback === 'play'
                ? i18n.study.whatWouldYouPlay
                : state.feedback === 'end' && i18n.study.youCompletedThisLesson,
            ),
        hintZone(ctrl),
      ]),
    h('div.floor', [
      renderFeedback(ctrl, state),
      h('img.mascot', {
        attrs: { width: 120, height: 120, src: site.asset.url('images/mascot/octopus.svg') },
      }),
    ]),
  ]);
}

function hintZone(ctrl: GamebookPlayCtrl) {
  const state = ctrl.state,
    buttonData = () => ({ attrs: { type: 'button' }, hook: bind('click', ctrl.hint, ctrl.redraw) });
  if (state.showHint) return h('button', buttonData(), [h('div.hint', { hook: richHTML(state.hint!) })]);
  if (state.hint) return h('button.hint', buttonData(), i18n.site.getAHint);
  return undefined;
}

function renderFeedback(ctrl: GamebookPlayCtrl, state: State) {
  const fb = state.feedback,
    color = ctrl.root.turnColor();
  if (fb === 'bad')
    return h(
      'button.feedback.act.bad' + (state.comment ? '.com' : ''),
      { attrs: { type: 'button' }, hook: bind('click', ctrl.retry) },
      [iconTag(licon.Reload), h('span', i18n.site.retry)],
    );
  if (fb === 'good' && state.comment)
    return h('button.feedback.act.good.com', { attrs: { type: 'button' }, hook: bind('click', ctrl.next) }, [
      h('span.text', { attrs: dataIcon(licon.PlayTriangle) }, i18n.study.next),
      h('kbd', '<space>'),
    ]);
  if (fb === 'end') return renderEnd(ctrl);
  return h(
    'div.feedback.info.' + fb + (state.init ? '.init' : ''),
    h(
      'div',
      fb === 'play'
        ? [
            h('div.no-square', h('piece.king.' + color)),
            h('div.instruction', [
              h('strong', i18n.site.yourTurn),
              h('em', i18n.puzzle[color === 'white' ? 'findTheBestMoveForWhite' : 'findTheBestMoveForBlack']),
            ]),
          ]
        : i18n.study.goodMove,
    ),
  );
}

function renderEnd(ctrl: GamebookPlayCtrl) {
  const study = ctrl.root.study!;
  return h('div.feedback.end', [
    study.nextChapter() &&
      h(
        'button.next.text',
        {
          attrs: { 'data-icon': licon.PlayTriangle, type: 'button' },
          hook: bind('click', study.goToNextChapter),
        },
        i18n.study.nextChapter,
      ),
    h(
      'button.retry',
      {
        attrs: { 'data-icon': licon.Reload, type: 'button' },
        hook: bind('click', () => ctrl.root.userJump(''), ctrl.redraw),
      },
      i18n.study.playAgain,
    ),
    h(
      'button.analyse',
      {
        attrs: { 'data-icon': licon.Microscope, type: 'button' },
        hook: bind('click', () => study.setGamebookOverride('analyse'), ctrl.redraw),
      },
      i18n.site.analysis,
    ),
  ]);
}
