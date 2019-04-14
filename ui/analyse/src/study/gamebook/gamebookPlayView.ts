import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import GamebookPlayCtrl from './gamebookPlayCtrl';
import { bind, dataIcon, iconTag, richHTML } from '../../util';
import { State } from './gamebookPlayCtrl';

const defaultComments = {
  play: 'whatWouldYouPlay',
  end: 'lessonCompleted'
};

export function render(ctrl: GamebookPlayCtrl): VNode {

  const state = ctrl.state,
    comment = state.comment || ctrl.trans.noarg(defaultComments[state.feedback]);

  return h('div.gamebook', {
    hook: { insert: _ => window.lidraughts.loadCssPath('analyse.gamebook.play') }
  }, [
    comment ? h('div.comment', {
      class: { hinted: state.showHint }
    }, [
      h('div.content', { hook: richHTML(comment) }),
      hintZone(ctrl)
    ]) : undefined,
    h('div.floor', [
      renderFeedback(ctrl, state),
      !ctrl.root.embed ? h('img.mascot', {
        attrs: {
          width: 120,
          height: 120,
          src: window.lidraughts.assetUrl('images/mascot/octopus.svg')
        }
      }) : null
    ])
  ]);
}

function hintZone(ctrl: GamebookPlayCtrl) {
  const state = ctrl.state,
  clickHook = () => ({
    hook: bind('click', ctrl.hint, ctrl.redraw)
  });
  if (state.showHint) return h('div', clickHook(), [
    h('div.hint', { hook: richHTML(state.hint!) })
  ]);
  if (state.hint) return h('a.hint', clickHook(), ctrl.trans.noarg('getAHint'));
}

function renderFeedback(ctrl: GamebookPlayCtrl, state: State) {
  const fb = state.feedback,
  color = ctrl.root.turnColor();
  if (fb === 'bad') return h('div.feedback.act.bad' + (state.comment ? '.com' : ''), {
    hook: bind('click', ctrl.retry)
  }, [
    iconTag('P'),
    h('span', ctrl.trans.noarg('retry'))
  ]);
  if (fb === 'good' && state.comment) return h('div.feedback.act.good.com', {
    hook: bind('click', ctrl.next)
  }, [
    h('span.text', { attrs: dataIcon('G') }, ctrl.trans.noarg('next')),
    h('kbd', '<space>')
  ]);
  if (fb === 'end') return renderEnd(ctrl);
  return h('div.feedback.info.' + fb + (state.init ? '.init' : ''),
    h('div', fb === 'play' ? [
      h('div.no-square', h('piece.king.' + color)),
      h('div.instruction', [
        h('strong', ctrl.trans.noarg('yourTurn')),
        h('em', ctrl.trans.noarg(color === 'white' ? 'findTheBestMoveForWhite' : 'findTheBestMoveForBlack'))
      ])
    ] : [ ctrl.trans.noarg('goodMove') ])
  );
}

function renderEnd(ctrl: GamebookPlayCtrl) {
  const study = ctrl.root.study!,
  nextChapter = study.nextChapter();
  return h('div.feedback.end', [
    nextChapter ? h('a.next.text', {
      attrs: dataIcon('G'),
      hook: bind('click', () => study.setChapter(nextChapter.id))
    }, ctrl.trans.noarg('nextChapter')) : undefined,
    h('a.retry', {
      attrs: dataIcon('P'),
      hook: bind('click', () => ctrl.root.userJump(''), ctrl.redraw)
    }, ctrl.trans.noarg('playAgain')),
    !ctrl.root.embed ? h('a.analyse', {
      attrs: dataIcon('A'),
      hook: bind('click', () => study.setGamebookOverride('analyse'), ctrl.redraw)
    }, ctrl.trans.noarg('analyse')) : null
  ]);
}
