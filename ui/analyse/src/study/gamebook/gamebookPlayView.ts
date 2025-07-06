import GamebookPlayCtrl, { type State } from './gamebookPlayCtrl';
import * as licon from 'lib/licon';
import { type VNode, iconTag, bind, dataIcon, hl } from 'lib/snabbdom';
import { richHTML } from 'lib/richText';

export function render(ctrl: GamebookPlayCtrl): VNode {
  const state = ctrl.state;
  return hl('div.gamebook', { hook: { insert: _ => site.asset.loadCssPath('analyse.gamebook.play') } }, [
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
      [iconTag(licon.Reload), hl('span', i18n.site.retry)],
    );
  if (fb === 'good' && state.comment)
    return hl('button.feedback.act.good.com', { attrs: { type: 'button' }, hook: bind('click', ctrl.next) }, [
      hl('span.text', { attrs: dataIcon(licon.PlayTriangle) }, i18n.study.next),
      hl('kbd', '<space>'),
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
              hl(
                'em',
                i18n.puzzle[color === 'white' ? 'findTheBestMoveForWhite' : 'findTheBestMoveForBlack'],
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
          attrs: { 'data-icon': licon.PlayTriangle, type: 'button' },
          hook: bind('click', study.goToNextChapter),
        },
        i18n.study.nextChapter,
      ),
    hl(
      'button.retry',
      {
        attrs: { 'data-icon': licon.Reload, type: 'button' },
        hook: bind('click', () => ctrl.root.userJump(''), ctrl.redraw),
      },
      i18n.study.playAgain,
    ),
    hl(
      'button.analyse',
      {
        attrs: { 'data-icon': licon.Microscope, type: 'button' },
        hook: bind('click', () => study.setGamebookOverride('analyse'), ctrl.redraw),
      },
      i18n.site.analysis,
    ),
  ]);
}
