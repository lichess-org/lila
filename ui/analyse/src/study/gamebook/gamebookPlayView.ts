import { icons } from 'lib/icons';
import { richHTML } from 'lib/richText';
import { type VNode, bind, hl, requiresI18n, onInsert, snabIcon } from 'lib/view';

import GamebookPlayCtrl, { type State } from './gamebookPlayCtrl';

export function render(ctrl: GamebookPlayCtrl): VNode {
  const state = ctrl.state;
  return hl('div.gamebook', { hook: onInsert(() => site.asset.loadCssPath('analyse.gamebook.play')) }, [
    (state.comment || state.feedback === 'play' || state.feedback === 'end') &&
      hl('div.comment', { class: { hinted: state.showHint } }, [
        state.comment
          ? hl('div.content', { hook: richHTML(state.comment) })
          : hl(
              'div.content',
              state.feedback === 'play'
                ? i18n.study.whatWouldYouPlay
                : state.feedback === 'end' && i18n.study.youCompletedThisLesson,
            ),
        hintZone(ctrl),
      ]),
    hl('div.floor', [
      renderFeedback(ctrl, state),
      hl('img.mascot', {
        attrs: { width: 120, height: 120, src: site.asset.url('images/mascot/octopus.svg') },
      }),
    ]),
  ]);
}

function hintZone(ctrl: GamebookPlayCtrl) {
  const state = ctrl.state,
    buttonData = () => ({ attrs: { type: 'button' }, hook: bind('click', ctrl.hint, ctrl.redraw) });
  if (state.showHint) return hl('button', buttonData(), [hl('div.hint', { hook: richHTML(state.hint!) })]);
  if (state.hint) return hl('button.hint', buttonData(), i18n.site.getAHint);
  return undefined;
}

function renderFeedback(ctrl: GamebookPlayCtrl, state: State) {
  const fb = state.feedback,
    color = ctrl.root.turnColor();
  if (fb === 'bad')
    return hl(
      'button.feedback.act.bad' + (state.comment ? '.com' : ''),
      { attrs: { type: 'button' }, hook: bind('click', ctrl.retry) },
      [snabIcon(icons.Reload), hl('span', i18n.site.retry)],
    );
  if (fb === 'good' && state.comment)
    return hl('button.feedback.act.good.com', { attrs: { type: 'button' }, hook: bind('click', ctrl.next) }, [
      hl('span.text', [snabIcon(icons.PlayTriangle, 'mirror-rtl'), i18n.study.next]),
      hl('kbd', 'space'),
    ]);
  if (fb === 'end') return renderEnd(ctrl);
  return hl(
    'div.feedback.info.' + fb + (state.init ? '.init' : ''),
    hl(
      'div',
      fb === 'play'
        ? [
            hl('div.no-square', hl('piece.king.' + color)),
            hl('div.instruction', [
              hl('strong', i18n.site.yourTurn),
              requiresI18n('puzzle', ctrl.redraw, cat =>
                hl('em', cat[color === 'white' ? 'findTheBestMoveForWhite' : 'findTheBestMoveForBlack']),
              ),
            ]),
          ]
        : i18n.study.goodMove,
    ),
  );
}

function renderEnd(ctrl: GamebookPlayCtrl) {
  const study = ctrl.root.study!;
  return hl('div.feedback.end', [
    study.nextChapter() &&
      hl(
        'button.next.text',
        {
          attrs: { type: 'button' },
          hook: bind('click', study.goToNextChapter),
        },
        [snabIcon(icons.PlayTriangle, 'mirror-rtl'), i18n.study.nextChapter],
      ),
    hl(
      'button.retry',
      {
        attrs: { type: 'button' },
        hook: bind('click', () => ctrl.root.userJump(''), ctrl.redraw),
      },
      [snabIcon(icons.Reload), i18n.study.playAgain],
    ),
    !study.vm.gamebookOverride &&
      hl(
        'button.analyse',
        {
          attrs: { type: 'button' },
          hook: bind('click', () => study.setGamebookOverride('analyse'), ctrl.redraw),
        },
        [snabIcon(icons.Microscope), i18n.site.analysis],
      ),
  ]);
}
