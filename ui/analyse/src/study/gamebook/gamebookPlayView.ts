import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Hooks } from 'snabbdom/hooks'
import GamebookPlayCtrl from './gamebookPlayCtrl';
import { bind, dataIcon, iconTag, enrichText, innerHTML } from '../../util';
import { State } from './gamebookPlayCtrl';

const defaultComments = {
  play: 'What would you play in this position?',
  end: 'Congratulations! You completed this lesson.'
};

export function render(ctrl: GamebookPlayCtrl): VNode {

  const state = ctrl.state,
  comment = state.comment || defaultComments[state.feedback];

  return h('div.gamebook', {
    hook: { insert: _ => window.lichess.loadCss('/assets/stylesheets/gamebook.play.css') }
  }, [
    comment ? h('div.comment', {
      class: { hinted: state.showHint }
    }, [
      h('div.content', { hook: richHTML(comment) }),
      hintZone(ctrl)
    ]) : undefined,
    h('div.floor', [
      renderFeedback(ctrl, state),
      h('img.mascot', {
        attrs: {
          width: 120,
          height: 120,
          src: window.lichess.assetUrl('/assets/images/mascot/octopus.svg')
        }
      })
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
  if (state.hint) return h('a.hint', clickHook(), 'Get a hint');
}

function renderFeedback(ctrl: GamebookPlayCtrl, state: State) {
  const fb = state.feedback;
  if (fb === 'bad') return h('div.feedback.act.bad' + (state.comment ? '.com' : ''), {
    hook: bind('click', ctrl.retry)
  }, [
    iconTag('P'),
    h('span', 'Retry')
  ]);
  if (fb === 'good' && state.comment) return h('div.feedback.act.good.com', {
    hook: bind('click', ctrl.next)
  }, [
    h('span.text', { attrs: dataIcon('G') }, 'Next'),
    h('kbd', '<space>')
  ]);
  if (fb === 'end') return renderEnd(ctrl);
  return h('div.feedback.info.' + fb + (state.init ? '.init' : ''),
    h('span', fb === 'play' ? 'Your turn' : 'Good move!')
  );
}

function renderEnd(ctrl: GamebookPlayCtrl) {
  const study = ctrl.root.study!,
  nextChapter = study.nextChapter();
  return h('div.feedback.end', [
    nextChapter ? h('a.next.text', {
      attrs: dataIcon('G'),
      hook: bind('click', () => study.setChapter(nextChapter.id))
    }, 'Next chapter') : undefined,
    h('a.retry', {
      attrs: dataIcon('P'),
      hook: bind('click', () => ctrl.root.userJump(''), ctrl.redraw)
    }, 'Play again'),
    h('a.analyse', {
      attrs: dataIcon('A'),
      hook: bind('click', () => study.setGamebookOverride('analyse'), ctrl.redraw)
    }, 'Analyse')
  ]);
}

function richHTML(text: string): Hooks {
  return innerHTML(text, text => enrichText(text, true, false));
}
