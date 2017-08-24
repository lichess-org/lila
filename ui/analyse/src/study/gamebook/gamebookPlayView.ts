import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Hooks } from 'snabbdom/hooks'
import GamebookPlayCtrl from './gamebookPlayCtrl';
import { bind, dataIcon, enrichText, innerHTML } from '../../util';
import { State } from './gamebookPlayCtrl';

const defaultComments = {
  play: 'What would you play in this position?',
  bad: 'That\'s not the right move.',
  end: 'Congratulations! You completed this gamebook.'
};

export function render(ctrl: GamebookPlayCtrl): VNode {

  const state = ctrl.state,
  comment = state.comment || defaultComments[state.feedback];

  return h('div.gamebook', {
    hook: { insert: _ => window.lichess.loadCss('/assets/stylesheets/gamebook.play.css') }
  }, [
    comment ? h('div.comment', [
      h('div.content', { hook: richHTML(comment) }),
      state.showHint ? h('div.hint', { hook: richHTML(state.hint!) }) : undefined
    ]) : undefined,
    h('img.mascot', {
      attrs: {
        width: 120,
        height: 120,
        src: ctrl.mascot.url(),
        title: 'Click to choose your teacher'
      },
      hook: bind('click', ctrl.mascot.switch, ctrl.redraw)
    }),
    renderFeedback(ctrl, state)
  ]);
}

function renderFeedback(ctrl: GamebookPlayCtrl, state: State) {
  const fb = state.feedback;
  if (fb === 'bad') return h('div.feedback.act.bad', {
    hook: bind('click', ctrl.retry, ctrl.redraw)
  }, [
    h('i', { attrs: dataIcon('P') }),
    h('span', 'Retry')
  ]);
  if (fb === 'good' && state.comment) return h('div.feedback.act.good', {
    hook: bind('click', () => ctrl.next())
  }, [
    h('i', { attrs: dataIcon('G') }),
    h('span', 'Next')
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
      hook: bind('click', () => study!.setChapter(nextChapter.id))
    }, 'Next chapter') : undefined,
    h('a.retry', {
      attrs: dataIcon('P'),
      hook: bind('click', () => ctrl.root.userJump(''), ctrl.redraw)
    }, 'Play again'),
    h('a.analyse', {
      attrs: dataIcon('A')
    }, 'Analyse')
  ]);
}

function richHTML(text: string): Hooks {
  return innerHTML(text, text => enrichText(text, true));
}
